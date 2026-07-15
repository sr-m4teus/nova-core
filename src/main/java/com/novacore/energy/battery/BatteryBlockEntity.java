package com.novacore.energy.battery;

import com.novacore.NovaCore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class BatteryBlockEntity extends BlockEntity {

    private final SimpleEnergyHandler energy;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(NovaCore.BATTERY_BLOCK_ENTITY.get(), pos, state);
        BatteryTier tier = ((BatteryBlock) state.getBlock()).tier();
        this.energy = new SimpleEnergyHandler(tier.capacity(), tier.maxTransfer()) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                setChanged();
            }
        };
    }

    public EnergyHandler energyHandler() {
        return energy;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
    }
}
