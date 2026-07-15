package com.novacore.energy.battery;

/**
 * Capacity and per-tick transfer limit for each battery tier, 10x scaling per step
 * (per the energy pillar spec's storage section).
 */
public enum BatteryTier {
    BASIC(100_000, 1_000),
    ADVANCED(1_000_000, 10_000),
    SUPREME(10_000_000, 100_000);

    private final int capacity;
    private final int maxTransfer;

    BatteryTier(int capacity, int maxTransfer) {
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    public int capacity() {
        return capacity;
    }

    public int maxTransfer() {
        return maxTransfer;
    }
}
