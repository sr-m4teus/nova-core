package com.novacore.energy.network;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Tracks which {@link EnergyNetwork} each node ({@code K}, typically a cable position) belongs
 * to, merging networks when a node is added next to existing ones and splitting a network into
 * its remaining connected components when a node is removed.
 *
 * <p>Also tracks which external endpoints ({@code E}, typically a neighbor position/side pair)
 * are anchored to which node, so that on a split each endpoint follows its anchor node into the
 * resulting component, and so that removing a node detaches the endpoints anchored to it.
 *
 * <p>Adjacency is supplied by the caller ({@code neighborLookup}), so this class has no notion of
 * world space and can be tested with plain node keys.
 */
public final class EnergyNetworkManager<K, E> {

    private final Function<K, ? extends Collection<K>> neighborLookup;
    private final Map<K, EnergyNetwork<K, E>> networkByNode = new HashMap<>();
    private final Map<K, Set<E>> endpointsByNode = new HashMap<>();
    private final Map<E, K> nodeByEndpoint = new HashMap<>();

    public EnergyNetworkManager(Function<K, ? extends Collection<K>> neighborLookup) {
        this.neighborLookup = neighborLookup;
    }

    public EnergyNetwork<K, E> networkOf(K node) {
        return networkByNode.get(node);
    }

    public Set<EnergyNetwork<K, E>> allNetworks() {
        return new LinkedHashSet<>(networkByNode.values());
    }

    public EnergyNetwork<K, E> addNode(K node) {
        Set<EnergyNetwork<K, E>> toMerge = new LinkedHashSet<>();
        for (K neighbor : neighborLookup.apply(node)) {
            EnergyNetwork<K, E> network = networkByNode.get(neighbor);
            if (network != null) {
                toMerge.add(network);
            }
        }

        EnergyNetwork<K, E> result = toMerge.isEmpty() ? new EnergyNetwork<>() : mergeAll(toMerge);
        result.addNode(node);
        networkByNode.put(node, result);
        return result;
    }

    private EnergyNetwork<K, E> mergeAll(Set<EnergyNetwork<K, E>> networks) {
        var it = networks.iterator();
        EnergyNetwork<K, E> target = it.next();
        while (it.hasNext()) {
            EnergyNetwork<K, E> other = it.next();
            for (K node : other.nodes()) {
                networkByNode.put(node, target);
            }
            target.absorb(other);
        }
        return target;
    }

    public void removeNode(K node) {
        EnergyNetwork<K, E> old = networkByNode.remove(node);
        if (old == null) {
            return;
        }

        for (E endpoint : endpointsByNode.getOrDefault(node, Set.of())) {
            old.takeProvider(endpoint);
            old.takeConsumer(endpoint);
            nodeByEndpoint.remove(endpoint);
        }
        endpointsByNode.remove(node);

        Set<K> remaining = new HashSet<>(old.nodes());
        remaining.remove(node);
        old.removeNode(node);

        Set<K> unvisited = new HashSet<>(remaining);
        while (!unvisited.isEmpty()) {
            K seed = unvisited.iterator().next();
            unvisited.remove(seed);

            EnergyNetwork<K, E> component = new EnergyNetwork<>();
            Deque<K> queue = new ArrayDeque<>();
            queue.add(seed);

            while (!queue.isEmpty()) {
                K current = queue.poll();
                component.addNode(current);
                networkByNode.put(current, component);

                for (E endpoint : endpointsByNode.getOrDefault(current, Set.of())) {
                    EnergyProvider provider = old.takeProvider(endpoint);
                    if (provider != null) component.attachProvider(endpoint, provider);
                    EnergyConsumer consumer = old.takeConsumer(endpoint);
                    if (consumer != null) component.attachConsumer(endpoint, consumer);
                }

                for (K neighbor : neighborLookup.apply(current)) {
                    if (remaining.contains(neighbor) && unvisited.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    public void attachProvider(K node, E endpoint, EnergyProvider provider) {
        requireNetwork(node).attachProvider(endpoint, provider);
        anchor(node, endpoint);
    }

    public void attachConsumer(K node, E endpoint, EnergyConsumer consumer) {
        requireNetwork(node).attachConsumer(endpoint, consumer);
        anchor(node, endpoint);
    }

    private void anchor(K node, E endpoint) {
        endpointsByNode.computeIfAbsent(node, n -> new LinkedHashSet<>()).add(endpoint);
        nodeByEndpoint.put(endpoint, node);
    }

    public void detach(E endpoint) {
        K node = nodeByEndpoint.remove(endpoint);
        if (node == null) {
            return;
        }

        EnergyNetwork<K, E> network = networkByNode.get(node);
        if (network != null) {
            network.detachProvider(endpoint);
            network.detachConsumer(endpoint);
        }

        Set<E> endpoints = endpointsByNode.get(node);
        if (endpoints != null) {
            endpoints.remove(endpoint);
            if (endpoints.isEmpty()) {
                endpointsByNode.remove(node);
            }
        }
    }

    private EnergyNetwork<K, E> requireNetwork(K node) {
        EnergyNetwork<K, E> network = networkByNode.get(node);
        if (network == null) {
            throw new IllegalStateException("Node " + node + " is not part of any network");
        }
        return network;
    }
}
