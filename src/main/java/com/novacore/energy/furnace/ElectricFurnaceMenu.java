package com.novacore.energy.furnace;

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

public class ElectricFurnaceMenu extends AbstractContainerMenu {

    private static final int DATA_COUNT = 4;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final Container container;
    private final ContainerData data;

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(NovaCore.ELECTRIC_FURNACE_MENU.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, INPUT_SLOT, 56, 35));
        this.addSlot(new OutputSlot(container, OUTPUT_SLOT, 116, 35));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    public int cookingProgress() {
        return data.get(0);
    }

    public int cookingTotalTime() {
        return data.get(1);
    }

    public int energyAmount() {
        return data.get(2);
    }

    public int energyCapacity() {
        return data.get(3);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            if (index == OUTPUT_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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

    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
