package com.novacore.energy;

/**
 * Fixed real-world-Joules-to-game-FE conversion, so every energy value added to this mod is
 * derived from actual physics instead of picked arbitrarily.
 *
 * <p>Anchor: bituminous coal's calorific value is ~24 MJ/kg. NovaCore treats 1 coal item as
 * ~1 kg, and the Thermal Generator's design target is for a single piece of coal (1600 ticks of
 * burn time, same as a vanilla furnace) to net roughly a third of a Basic Battery's capacity
 * (100,000 FE). That fixes the scale at 1 FE = 750 J:
 *
 * <pre>
 *   24,000,000 J / 750 J-per-FE  = 32,000 FE per coal
 *   32,000 FE / 1600 ticks       = 20 FE/tick generation rate
 * </pre>
 *
 * <p>Every later mechanic (solar irradiance, reactor output, electric furnace smelting cost, ...)
 * should derive its FE values through this same constant rather than introducing a second,
 * inconsistent scale.
 */
public final class EnergyScale {
    public static final double JOULES_PER_FE = 750.0;

    public static long feFromJoules(double joules) {
        return Math.round(joules / JOULES_PER_FE);
    }

    private EnergyScale() {}
}
