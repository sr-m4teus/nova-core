package com.novacore.energy.generator;

import com.novacore.NovaCore;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ThermalGeneratorMenu extends AbstractContainerMenu {

    private static final int DATA_COUNT = 4;
    private static final int FUEL_SLOT = 0;

    private final Container fuelContainer;
    private final ContainerData data;

    public ThermalGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    public ThermalGeneratorMenu(int containerId, Inventory playerInventory, Container fuelContainer, ContainerData data) {
        super(NovaCore.THERMAL_GENERATOR_MENU.get(), containerId);
        checkContainerSize(fuelContainer, 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.fuelContainer = fuelContainer;
        this.data = data;

        Level level = playerInventory.player.level();
        this.addSlot(new FuelSlot(fuelContainer, FUEL_SLOT, 80, 41, level));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    public int burnTicksRemaining() {
        return data.get(0);
    }

    public int totalBurnTicks() {
        return data.get(1);
    }

    public int energyAmount() {
        return data.get(2);
    }

    public int energyCapacity() {
        return data.get(3);
    }

    public boolean isBurning() {
        return burnTicksRemaining() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return fuelContainer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            if (index == FUEL_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(FUEL_SLOT).mayPlace(stackInSlot) && !this.getSlot(FUEL_SLOT).hasItem()) {
                if (!this.moveItemStackTo(stackInSlot, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    private static final class FuelSlot extends Slot {
        private final Level level;

        FuelSlot(Container container, int index, int x, int y, Level level) {
            super(container, index, x, y);
            this.level = level;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getBurnTime(null, level.fuelValues()) > 0;
        }
    }
}
