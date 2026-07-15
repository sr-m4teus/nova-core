package com.novacore.energy.furnace;

import java.util.Optional;

import com.novacore.NovaCore;
import com.novacore.energy.EnergyScale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Runs vanilla smelting recipes on electricity instead of item fuel. The per-tick cost is derived
 * through {@link EnergyScale}, anchored on the real energy needed to heat and melt ~1 kg of iron
 * (a stand-in for "smelt one item"):
 *
 * <pre>
 *   heat 1 kg iron 20C -&gt; 1538C: 0.449 J/g*C * 1000 g * 1518 C  ~= 681,582 J
 *   latent heat of fusion:        247 J/g * 1000 g               = 247,000 J
 *   total                                                       ~= 928,600 J
 *   928,600 J / 750 J-per-FE                                    ~= 1238 FE per smelt
 *   1238 FE / 200 ticks (vanilla smelting time)                 ~= 6.2 FE/tick -&gt; rounded to 6
 * </pre>
 *
 * That number applies uniformly to every smelting recipe (same simplification the Thermal
 * Generator makes with a single coal-derived FE/tick for all furnace fuels): a Thermal Generator
 * alone comfortably powers one furnace (20 vs 6 FE/tick); a single Solar Panel alone cannot
 * (2.667 peak vs 6), which is the intended, physically-grounded progression.
 */
public class ElectricFurnaceBlockEntity extends BaseContainerBlockEntity {

    public static final int ENERGY_PER_TICK = 6;
    private static final int BUFFER_CAPACITY = 200;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int cookingProgress;
    private int cookingTotalTime = 200;

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.SMELTING);

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(BUFFER_CAPACITY, BUFFER_CAPACITY, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookingProgress;
                case 1 -> cookingTotalTime;
                case 2 -> energy.getAmountAsInt();
                case 3 -> energy.getCapacityAsInt();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookingProgress = value;
                case 1 -> cookingTotalTime = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(NovaCore.ELECTRIC_FURNACE_BLOCK_ENTITY.get(), pos, state);
    }

    public EnergyHandler energyHandler() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity furnace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack input = furnace.items.get(INPUT_SLOT);
        Optional<RecipeHolder<SmeltingRecipe>> match = input.isEmpty()
                ? Optional.empty()
                : furnace.quickCheck.getRecipeFor(new SingleRecipeInput(input), serverLevel);

        if (match.isEmpty()) {
            if (furnace.cookingProgress != 0) {
                furnace.cookingProgress = 0;
                furnace.setChanged();
            }
            return;
        }

        SmeltingRecipe recipe = match.get().value();
        ItemStack result = recipe.assemble(new SingleRecipeInput(input));
        if (!furnace.canInsertResult(result)) {
            furnace.cookingProgress = 0;
            furnace.setChanged();
            return;
        }

        if (furnace.energy.getAmountAsInt() < ENERGY_PER_TICK) {
            return;
        }

        furnace.energy.set(furnace.energy.getAmountAsInt() - ENERGY_PER_TICK);
        furnace.cookingTotalTime = recipe.cookingTime();
        furnace.cookingProgress++;

        if (furnace.cookingProgress >= furnace.cookingTotalTime) {
            furnace.cookingProgress = 0;
            furnace.insertResult(result);
            input.shrink(1);
        }

        furnace.setChanged();
    }

    private boolean canInsertResult(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void insertResult(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.novacore.electric_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ElectricFurnaceMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("CookingProgress", cookingProgress);
        output.putInt("CookingTotalTime", cookingTotalTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        cookingProgress = input.getIntOr("CookingProgress", 0);
        cookingTotalTime = input.getIntOr("CookingTotalTime", 200);
    }
}
