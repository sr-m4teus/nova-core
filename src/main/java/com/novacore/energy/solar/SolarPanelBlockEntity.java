package com.novacore.energy.solar;

import com.novacore.NovaCore;
import com.novacore.energy.EnergyScale;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Generates FE from sunlight. Output at any given tick is the peak rate times three independent
 * 0-1 factors: how high the sun is (day/night), how much sky is visible above the panel
 * (obstruction), and whether it's raining on the panel (weather).
 *
 * <p>Peak rate derivation -- see {@link EnergyScale}: a compact solar farm block is assumed to
 * represent ~200 m^2 of panels at ~20% real-world photovoltaic efficiency, under the standard
 * 1000 W/m^2 peak irradiance test condition.
 * <pre>
 *   200 m^2 * 1000 W/m^2 * 0.20 = 40,000 W peak = 2,000 J/tick (at 20 ticks/s)
 *   2,000 J / 750 J-per-FE      = 2.667 FE/tick peak
 * </pre>
 * That's deliberately far below the Thermal Generator's 20 FE/tick: solar is diffuse and free,
 * combustion is concentrated and needs fuel -- a real physical trade-off, not a balance fudge.
 */
public class SolarPanelBlockEntity extends BlockEntity {

    private static final double PEAK_FE_PER_TICK = 2000.0 / EnergyScale.JOULES_PER_FE;
    private static final float RAIN_OUTPUT_FACTOR = 0.2F;
    private static final int BUFFER_CAPACITY = 200;
    private static final int BUFFER_MAX_EXTRACT = 50;

    private double fractionalEnergy;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(BUFFER_CAPACITY, 0, BUFFER_MAX_EXTRACT) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(NovaCore.SOLAR_PANEL_BLOCK_ENTITY.get(), pos, state);
    }

    public EnergyHandler energyHandler() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        if (panel.energy.getAmountAsInt() >= panel.energy.getCapacityAsInt()) {
            return;
        }

        float dayFactor = 1.0F - level.getSkyDarken() / 11.0F;
        float skyExposure = level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos) / 15.0F;
        float rainFactor = level.isRainingAt(pos) ? RAIN_OUTPUT_FACTOR : 1.0F;

        double produced = PEAK_FE_PER_TICK * dayFactor * skyExposure * rainFactor;
        if (produced <= 0) {
            return;
        }

        panel.fractionalEnergy += produced;
        int wholeFe = (int) panel.fractionalEnergy;
        if (wholeFe > 0) {
            panel.fractionalEnergy -= wholeFe;
            int room = panel.energy.getCapacityAsInt() - panel.energy.getAmountAsInt();
            panel.energy.set(panel.energy.getAmountAsInt() + Math.min(wholeFe, room));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putDouble("FractionalEnergy", fractionalEnergy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        fractionalEnergy = input.getDoubleOr("FractionalEnergy", 0.0);
    }
}
