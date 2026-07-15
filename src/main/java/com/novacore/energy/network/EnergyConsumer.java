package com.novacore.energy.network;

public interface EnergyConsumer {
    long getDemand();

    long insert(long amount);
}
