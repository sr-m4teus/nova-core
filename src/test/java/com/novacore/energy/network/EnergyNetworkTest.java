package com.novacore.energy.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyNetworkTest {

    @Test
    void tickIsNoOpWithNoConsumers() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.attachProvider(1, spyProvider(100));

        var result = network.tick();

        assertEquals(0, result.totalSupplied());
        assertEquals(0, result.totalDemanded());
        assertEquals(0, result.totalTransferred());
    }

    @Test
    void tickIsNoOpWhenTotalDemandIsZero() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        network.attachProvider(1, spyProvider(100));
        network.attachConsumer(2, spyConsumer(0));

        var result = network.tick();

        assertEquals(0, result.totalTransferred());
    }

    @Test
    void singleProviderSingleConsumerTransfersExactAmount() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        var provider = new TrackingProvider(1000);
        var consumer = new TrackingConsumer(300);
        network.attachProvider(1, provider);
        network.attachConsumer(2, consumer);

        var result = network.tick();

        assertEquals(300, result.totalTransferred());
        assertEquals(300, consumer.received);
        assertEquals(700, provider.available);
    }

    @Test
    void demandExceedingSupplyIsCappedAtAvailable() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        var provider = new TrackingProvider(100);
        var consumer = new TrackingConsumer(1000);
        network.attachProvider(1, provider);
        network.attachConsumer(2, consumer);

        var result = network.tick();

        assertEquals(100, result.totalTransferred());
        assertEquals(100, consumer.received);
        assertEquals(0, provider.available);
    }

    @Test
    void supplyExceedingDemandIsCappedAtDemand() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        var provider = new TrackingProvider(1000);
        var consumer = new TrackingConsumer(100);
        network.attachProvider(1, provider);
        network.attachConsumer(2, consumer);

        var result = network.tick();

        assertEquals(100, result.totalTransferred());
        assertEquals(900, provider.available);
    }

    @Test
    void scarceSupplyIsSplitProportionallyToDemandAndConservesExactTotal() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        network.addNode(3);
        var provider = new TrackingProvider(100);
        var bigConsumer = new TrackingConsumer(300);
        var smallConsumer = new TrackingConsumer(100);
        network.attachProvider(1, provider);
        network.attachConsumer(2, bigConsumer);
        network.attachConsumer(3, smallConsumer);

        var result = network.tick();

        // 400 total demand, 100 available -> 75/25 split, exact sum preserved.
        assertEquals(100, result.totalTransferred());
        assertEquals(75, bigConsumer.received);
        assertEquals(25, smallConsumer.received);
        assertEquals(0, provider.available);
    }

    @Test
    void multipleProvidersAreDrawnProportionallyToAvailability() {
        var network = new EnergyNetwork<Integer>();
        network.addNode(1);
        network.addNode(2);
        network.addNode(3);
        var bigProvider = new TrackingProvider(300);
        var smallProvider = new TrackingProvider(100);
        var consumer = new TrackingConsumer(100);
        network.attachProvider(1, bigProvider);
        network.attachProvider(2, smallProvider);
        network.attachConsumer(3, consumer);

        var result = network.tick();

        assertEquals(100, result.totalTransferred());
        // 400 total available, drawing 100 -> 75/25 split, exact sum preserved.
        assertEquals(225, bigProvider.available);
        assertEquals(75, smallProvider.available);
    }

    private static EnergyProvider spyProvider(long available) {
        return new TrackingProvider(available);
    }

    private static EnergyConsumer spyConsumer(long demand) {
        return new TrackingConsumer(demand);
    }

    private static final class TrackingProvider implements EnergyProvider {
        long available;

        TrackingProvider(long available) {
            this.available = available;
        }

        @Override
        public long getAvailable() {
            return available;
        }

        @Override
        public long extract(long amount) {
            long extracted = Math.min(amount, available);
            available -= extracted;
            return extracted;
        }
    }

    private static final class TrackingConsumer implements EnergyConsumer {
        long demand;
        long received;

        TrackingConsumer(long demand) {
            this.demand = demand;
        }

        @Override
        public long getDemand() {
            return demand;
        }

        @Override
        public long insert(long amount) {
            long accepted = Math.min(amount, demand);
            demand -= accepted;
            received += accepted;
            return accepted;
        }
    }
}
