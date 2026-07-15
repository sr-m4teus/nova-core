package com.novacore.energy.cable;

import com.novacore.energy.network.EnergyConsumer;
import com.novacore.energy.network.EnergyProvider;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Adapts a real NeoForge {@link EnergyHandler} to the pure-logic network's provider/consumer interfaces. */
final class EnergyHandlerAdapter implements EnergyProvider, EnergyConsumer {

    private final EnergyHandler handler;

    EnergyHandlerAdapter(EnergyHandler handler) {
        this.handler = handler;
    }

    @Override
    public long getAvailable() {
        return handler.getAmountAsLong();
    }

    @Override
    public long getDemand() {
        return Math.max(0, handler.getCapacityAsLong() - handler.getAmountAsLong());
    }

    @Override
    public long extract(long amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(clampToInt(amount), transaction);
            transaction.commit();
            return extracted;
        }
    }

    @Override
    public long insert(long amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(clampToInt(amount), transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }
}
