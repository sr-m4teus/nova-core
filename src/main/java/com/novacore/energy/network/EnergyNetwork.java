package com.novacore.energy.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One connected cluster of cables plus the providers/consumers attached to it.
 * No energy is lost in transport: {@link #tick()} distributes exactly what
 * providers report available, capped by what consumers report they demand.
 */
public final class EnergyNetwork<K> {

    public record DistributionResult(long totalSupplied, long totalDemanded, long totalTransferred) {
        static final DistributionResult EMPTY = new DistributionResult(0, 0, 0);
    }

    private final Set<K> nodes = new LinkedHashSet<>();
    private final Map<K, EnergyProvider> providers = new LinkedHashMap<>();
    private final Map<K, EnergyConsumer> consumers = new LinkedHashMap<>();

    void addNode(K node) {
        nodes.add(node);
    }

    void removeNode(K node) {
        nodes.remove(node);
        providers.remove(node);
        consumers.remove(node);
    }

    void absorb(EnergyNetwork<K> other) {
        nodes.addAll(other.nodes);
        providers.putAll(other.providers);
        consumers.putAll(other.consumers);
    }

    EnergyProvider takeProvider(K node) {
        return providers.remove(node);
    }

    EnergyConsumer takeConsumer(K node) {
        return consumers.remove(node);
    }

    public boolean contains(K node) {
        return nodes.contains(node);
    }

    public Set<K> nodes() {
        return Set.copyOf(nodes);
    }

    public void attachProvider(K node, EnergyProvider provider) {
        providers.put(node, provider);
    }

    public void attachConsumer(K node, EnergyConsumer consumer) {
        consumers.put(node, consumer);
    }

    public void detach(K node) {
        providers.remove(node);
        consumers.remove(node);
    }

    public DistributionResult tick() {
        if (consumers.isEmpty()) {
            return DistributionResult.EMPTY;
        }

        Map<K, Long> demand = new LinkedHashMap<>();
        long totalDemand = 0;
        for (var entry : consumers.entrySet()) {
            long d = Math.max(0, entry.getValue().getDemand());
            demand.put(entry.getKey(), d);
            totalDemand += d;
        }
        if (totalDemand == 0) {
            return DistributionResult.EMPTY;
        }

        Map<K, Long> available = new LinkedHashMap<>();
        long totalAvailable = 0;
        for (var entry : providers.entrySet()) {
            long a = Math.max(0, entry.getValue().getAvailable());
            available.put(entry.getKey(), a);
            totalAvailable += a;
        }

        long transferable = Math.min(totalDemand, totalAvailable);
        if (transferable <= 0) {
            return new DistributionResult(0, totalDemand, 0);
        }

        long totalInserted = 0;
        for (var share : largestRemainderShares(demand, transferable).entrySet()) {
            if (share.getValue() <= 0) continue;
            totalInserted += consumers.get(share.getKey()).insert(share.getValue());
        }

        long totalExtracted = 0;
        for (var share : largestRemainderShares(available, totalInserted).entrySet()) {
            if (share.getValue() <= 0) continue;
            totalExtracted += providers.get(share.getKey()).extract(share.getValue());
        }

        return new DistributionResult(totalExtracted, totalDemand, totalInserted);
    }

    /**
     * Splits {@code total} across {@code weights} proportionally, using the largest-remainder
     * method so the shares sum to exactly {@code total} instead of losing units to rounding.
     */
    private static <K> Map<K, Long> largestRemainderShares(Map<K, Long> weights, long total) {
        long totalWeight = 0;
        for (long w : weights.values()) totalWeight += w;

        Map<K, Long> shares = new LinkedHashMap<>();
        if (totalWeight <= 0 || total <= 0) {
            for (K k : weights.keySet()) shares.put(k, 0L);
            return shares;
        }

        Map<K, Long> remainders = new LinkedHashMap<>();
        long allocated = 0;
        for (var entry : weights.entrySet()) {
            long share = (total * entry.getValue()) / totalWeight;
            long remainder = (total * entry.getValue()) % totalWeight;
            shares.put(entry.getKey(), share);
            remainders.put(entry.getKey(), remainder);
            allocated += share;
        }

        long leftover = total - allocated;
        List<K> byRemainder = new ArrayList<>(weights.keySet());
        byRemainder.sort((a, b) -> Long.compare(remainders.get(b), remainders.get(a)));
        for (int i = 0; i < leftover; i++) {
            K key = byRemainder.get(i % byRemainder.size());
            shares.merge(key, 1L, Long::sum);
        }

        return shares;
    }
}
