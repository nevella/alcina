package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;
import cc.alcina.framework.classmeta.rdb.Packet.PacketPair;
import cc.alcina.framework.common.client.util.Ax;

interface PacketEndpointHost {
    void addPredictivePackets(List<Packet> predictivePackets);

    PacketEndpoint packetEndpoint();

    default void receivePacket(Packet packet) {
        packet.source = packetEndpoint();
        packetEndpoint().addInPacket(packet);
    }

    void send();

    default boolean shouldSend() {
        return true;
    }

    static class PacketEndpoint {
        PacketEndpointHost host;

        List<Packet> outPackets = new ArrayList<>();

        Map<Integer, Integer> originalToPredictiveIds = new LinkedHashMap<>();

        Map<Integer, Integer> predictiveToOriginalIds = new LinkedHashMap<>();

        Map<Integer, String> predictiveToOriginalMessageNames = new LinkedHashMap<>();

        Map<Integer, EventSeries> predictiveToOriginalPredictiveFor = new LinkedHashMap<>();

        int predictiveIdCounter = -1;

        LinkedList<Packet> inPackets = new LinkedList<>();

        Packets allInPackets = new Packets();

        List<Packet> predictivePacketsOutBuffer = new ArrayList<>();

        private List<Packet> currentPredictivePacketsHit = new ArrayList<>();

        Packets usablePredictiveReplies = new Packets();

        // starts true => i.e. no predictive packets we expect to be usable
        AtomicBoolean predictivePacketMissMonitor = new AtomicBoolean(true);

        private Endpoint endpoint;

        public PacketEndpoint(PacketEndpointHost host, Endpoint endpoint) {
            this.host = host;
            this.endpoint = endpoint;
        }

        public synchronized Packet createForPredictiveSequence(Packet packet) {
            if (predictiveIdCounter == -1) {
                predictiveIdCounter = packet.id() + 2 << 21;
            }
            int mappedId = predictiveIdCounter++;
            originalToPredictiveIds.put(packet.id(), mappedId);
            predictiveToOriginalIds.put(mappedId, packet.id());
            predictiveToOriginalMessageNames.put(mappedId, packet.messageName);
            predictiveToOriginalPredictiveFor.put(mappedId,
                    packet.predictiveFor);
            Packet copy = packet.copy();
            copy.setId(mappedId);
            predictivePacketsOutBuffer.add(copy);
            return copy;
        }

        public synchronized List<Packet> flushPredictivePackets() {
            List<Packet> result = predictivePacketsOutBuffer;
            predictivePacketsOutBuffer = new ArrayList<>();
            return result;
        }

        @Override
        public synchronized String toString() {
            return host.toString();
        }

        synchronized void addInPacket(Packet packet) {
            if (predictiveToOriginalIds.containsKey(packet.id())) {
                packet.isPredictive = true;
                packet.isReply = true;
                packet.messageName = predictiveToOriginalMessageNames
                        .get(packet.id());
                packet.predictiveFor = predictiveToOriginalPredictiveFor
                        .get(packet.id());
            }
            inPackets.add(packet);
            allInPackets.add(packet);
            endpoint.packetsReceived(packet);
        }

        synchronized void addOutPacket(Packet packet) {
            outPackets.add(packet);
        }

        void addReplyPacket(Packet translated) {
            addOutPacket(translated);
        }

        synchronized void clearPredictivePacketsForPayload(Packet packet) {
            usablePredictiveReplies.byPayloadResponse(packet);
        }

        void close() {
            endpoint.close();
        }

        boolean containsResponse(Packet packet) {
            return false;
        }

        synchronized List<Packet> currentPredictivePacketsHit() {
            return currentPredictivePacketsHit;
        }

        void ensureMessageName(Packet packet) {
            Packet command = allInPackets.byId(packet.id(), true);
            if (command != null) {
                packet.messageName = command.messageName;
            }
        }

        synchronized List<Packet> flushOutPackets() {
            List<Packet> flush = outPackets;
            outPackets = new ArrayList<>();
            return flush;
        }

        synchronized Packet getCorrespondingCommandPacket(Packet packet) {
            return allInPackets.byId(packet.id(), true);
        }

        synchronized List<PacketPair> getMostRecentPredictivePacketList() {
            return usablePredictiveReplies.streamRecentReplies()
                    .filter(p -> p.isReply)
                    .map(usablePredictiveReplies::toPacketPair)
                    .collect(Collectors.toList());
        }

        List<PacketPair> getPredictivePacketsLike(Packet like) {
            return usablePredictiveReplies.streamByName(like.messageName)
                    .filter(p -> p.isReply)
                    .map(usablePredictiveReplies::toPacketPair)
                    .collect(Collectors.toList());
        }

        synchronized Optional<Packet> getPredictiveResponse(Packet packet) {
            Optional<Packet> byPayloadResponse = usablePredictiveReplies
                    .byPayloadResponse(packet);
            Optional<Packet> predictiveResponse = byPayloadResponse
                    .map(response -> {
                        response = response.copy();
                        response.setId(packet.id());
                        return response;
                    });
            if (predictiveResponse.isPresent()) {
                currentPredictivePacketsHit.add(predictiveResponse.get());
            }
            return predictiveResponse;
        }

        synchronized Packet next() {
            return inPackets.isEmpty() ? null : inPackets.pop();
        }

        synchronized Packet nextIncomingPredictivePacket() {
            Iterator<Packet> itr = inPackets.iterator();
            while (itr.hasNext()) {
                Packet next = itr.next();
                if (next.isPredictive) {
                    itr.remove();
                    return next;
                }
            }
            return null;
        }

        synchronized void onPredictivePacketMiss() {
            if (!usablePredictiveReplies.hasPackets()) {
                return;
            }
            usablePredictiveReplies
                    .removeIf(packet -> !endpoint.oracle.isCacheable(packet));
            synchronized (predictivePacketMissMonitor) {
                predictivePacketMissMonitor.set(true);
                predictivePacketMissMonitor.notify();
            }
            currentPredictivePacketsHit.clear();
        }

        PacketEndpoint otherPacketEndpoint() {
            return endpoint.otherPacketEndpoint(this);
        }

        synchronized int outPacketCount() {
            return outPackets.size();
        }

        synchronized void receivedPredictivePackets(
                List<Packet> predictivePackets) {
            usablePredictiveReplies.clearRecentList();
            predictivePackets.forEach(usablePredictiveReplies::add);
            if (predictivePackets.size() > 0) {
                synchronized (predictivePacketMissMonitor) {
                    predictivePacketMissMonitor.set(false);
                }
            }
        }

        void send() {
            host.send();
        }

        synchronized boolean shouldSend() {
            return outPackets.size() > 0 && host.shouldSend();
        }

        synchronized Packet translateToOriginalId(Packet packet) {
            if (!predictiveToOriginalIds.containsKey(packet.id())) {
                throw Ax.runtimeException("Predictive id not found: %s",
                        packet.id());
            }
            predictivePacketsOutBuffer.add(packet);
            Packet copy = packet.copy();
            copy.setId(predictiveToOriginalIds.get(packet.id()));
            return copy;
        }

        void waitForPredictivePacketMiss() {
            synchronized (predictivePacketMissMonitor) {
                while (!predictivePacketMissMonitor.get()) {
                    try {
                        predictivePacketMissMonitor.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
