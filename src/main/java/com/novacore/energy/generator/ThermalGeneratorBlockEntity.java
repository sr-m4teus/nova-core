package com.novacore.energy.generator;

import com.novacore.NovaCore;
import com.novacore.energy.EnergyScale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Burns any registered furnace fuel into FE at a fixed rate derived from coal's real calorific
 * value -- see {@link EnergyScale}. The internal buffer is deliberately small: it exists only to
 * smooth out single-tick spikes, not to let the generator function as a battery.
 */
public class ThermalGeneratorBlockEntity extends BaseContainerBlockEntity {

    public static final int OUTPUT_PER_TICK = 20;
    private static final int BUFFER_CAPACITY = 4_000;
    private static final int BUFFER_MAX_EXTRACT = 200;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int burnTicksRemaining;
    private int totalBurnTicks;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(BUFFER_CAPACITY, 0, BUFFER_MAX_EXTRACT) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTicksRemaining;
                case 1 -> totalBurnTicks;
                case 2 -> energy.getAmountAsInt();
                case 3 -> energy.getCapacityAsInt();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTicksRemaining = value;
                case 1 -> totalBurnTicks = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ThermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(NovaCore.THERMAL_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public EnergyHandler energyHandler() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalGeneratorBlockEntity generator) {
        if (generator.burnTicksRemaining <= 0) {
            ItemStack fuel = generator.items.get(0);
            int burnTime = fuel.getBurnTime(null, level.fuelValues());
            if (!fuel.isEmpty() && burnTime > 0 && generator.energy.getAmountAsInt() < generator.energy.getCapacityAsInt()) {
                generator.burnTicksRemaining = burnTime;
                generator.totalBurnTicks = burnTime;
                fuel.shrink(1);
                generator.setChanged();
            }
        }

        if (generator.burnTicksRemaining > 0) {
            generator.burnTicksRemaining--;
            int produced = Math.min(OUTPUT_PER_TICK, generator.energy.getCapacityAsInt() - generator.energy.getAmountAsInt());
            if (produced > 0) {
                generator.energy.set(generator.energy.getAmountAsInt() + produced);
            }
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
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level != null && stack.getBurnTime(null, level.fuelValues()) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.novacore.thermal_generator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ThermalGeneratorMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("BurnTime", burnTicksRemaining);
        output.putInt("BurnTimeTotal", totalBurnTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        burnTicksRemaining = input.getIntOr("BurnTime", 0);
        totalBurnTicks = input.getIntOr("BurnTimeTotal", 0);
    }
}
