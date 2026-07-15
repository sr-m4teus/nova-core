package com.novacore.energy.network;

public interface EnergyProvider {
    long getAvailable();

    long extract(long amount);
}
