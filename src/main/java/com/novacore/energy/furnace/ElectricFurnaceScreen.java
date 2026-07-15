package com.novacore.energy.furnace;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * No Blockbench/texture art exists for this GUI yet, so the panel is drawn procedurally
 * (flat rectangles) as a functional placeholder pending a real background texture.
 */
@OnlyIn(Dist.CLIENT)
public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_COLOR = 0xFF373737;
    private static final int SLOT_COLOR = 0xFF8B8B8B;
    private static final int PROGRESS_COLOR = 0xFF50A050;
    private static final int PROGRESS_BG_COLOR = 0xFF404040;
    private static final int ENERGY_COLOR = 0xFFF0A020;
    private static final int ENERGY_BG_COLOR = 0xFF404040;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_COLOR);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, BORDER_COLOR);
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, BORDER_COLOR);
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);

        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT_COLOR);
        }

        int arrowX = leftPos + 79;
        int arrowY = topPos + 39;
        int arrowWidth = 24;
        graphics.fill(arrowX, arrowY, arrowX + arrowWidth, arrowY + 8, PROGRESS_BG_COLOR);
        int totalTime = menu.cookingTotalTime();
        int filled = totalTime > 0 ? arrowWidth * menu.cookingProgress() / totalTime : 0;
        graphics.fill(arrowX, arrowY, arrowX + filled, arrowY + 8, PROGRESS_COLOR);

        int barX = leftPos + 152;
        int barY = topPos + 17;
        int barHeight = 52;
        graphics.fill(barX, barY, barX + 12, barY + barHeight, ENERGY_BG_COLOR);
        int capacity = menu.energyCapacity();
        int energyFilled = capacity > 0 ? barHeight * menu.energyAmount() / capacity : 0;
        graphics.fill(barX, barY + barHeight - energyFilled, barX + 12, barY + barHeight, ENERGY_COLOR);
    }
}
