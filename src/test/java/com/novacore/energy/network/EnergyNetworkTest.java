package com.novacore.energy.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyNetworkTest {

    @Test
    void tickIsNoOpWithNoConsumers() {
        var network = new EnergyNetwork<Integer, Integer>();
        network.addNode(1);
        network.attachProvider(1, spyProvider(100));

        var result = network.tick();

        assertEquals(0, result.totalSupplied());
        assertEquals(0, result.totalDemanded());
        assertEquals(0, result.totalTransferred());
    }

    @Test
    void tickIsNoOpWhenTotalDemandIsZero() {
        var network = new EnergyNetwork<Integer, Integer>();
        network.addNode(1);
        network.addNode(2);
        network.attachProvider(1, spyProvider(100));
        network.attachConsumer(2, spyConsumer(0));

        var result = network.tick();

        assertEquals(0, result.totalTransferred());
    }

    @Test
    void singleProviderSingleConsumerTransfersExactAmount() {
        var network = new EnergyNetwork<Integer, Integer>();
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
        var network = new EnergyNetwork<Integer, Integer>();
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
        var network = new EnergyNetwork<Integer, Integer>();
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
        var network = new EnergyNetwork<Integer, Integer>();
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
        var network = new EnergyNetwork<Integer, Integer>();
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

    @Test
    void unextractableProviderCannotInflateWhatGetsDelivered() {
        // Regression test: a node that's registered as both provider and consumer (like every
        // real cable endpoint is -- the network doesn't know ahead of time which role a handler
        // actually supports) can report a nonzero getAvailable() from its own stored energy while
        // rejecting every real extract() call, e.g. a consumer-only device like the electric
        // furnace (maxExtract=0). Before this was fixed, tick() summed getAvailable() into
        // totalAvailable to size the insert to consumers *before* attempting any real extraction,
        // so this stored-but-unextractable amount was delivered to other consumers anyway --
        // energy created from nothing.
        var network = new EnergyNetwork<Integer, Integer>();
        network.addNode(1);
        network.addNode(2);
        var deadEndProvider = new UnextractableProvider(500);
        var consumer = new TrackingConsumer(1000);
        network.attachProvider(1, deadEndProvider);
        network.attachConsumer(2, consumer);

        var result = network.tick();

        assertEquals(0, result.totalTransferred());
        assertEquals(0, consumer.received);
    }

    @Test
    void realProviderStillSuppliesEvenWhenAnUnextractableProviderIsAlsoPresent() {
        var network = new EnergyNetwork<Integer, Integer>();
        network.addNode(1);
        network.addNode(2);
        network.addNode(3);
        var deadEndProvider = new UnextractableProvider(500);
        var realProvider = new TrackingProvider(100);
        var consumer = new TrackingConsumer(1000);
        network.attachProvider(1, deadEndProvider);
        network.attachProvider(2, realProvider);
        network.attachConsumer(3, consumer);

        var result = network.tick();

        assertEquals(100, result.totalTransferred());
        assertEquals(100, consumer.received);
        assertEquals(0, realProvider.available);
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

    /** Reports stored energy via getAvailable() but never actually yields it, like a
     * consumer-only device (maxExtract=0) attached to a cable that treats every endpoint as a
     * potential provider. */
    private static final class UnextractableProvider implements EnergyProvider {
        private final long available;

        UnextractableProvider(long available) {
            this.available = available;
        }

        @Override
        public long getAvailable() {
            return available;
        }

        @Override
        public long extract(long amount) {
            return 0;
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
