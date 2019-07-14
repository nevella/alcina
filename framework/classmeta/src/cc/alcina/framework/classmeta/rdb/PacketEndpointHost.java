package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

interface PacketEndpointHost {
    PacketEndpoint endpoint();

    default void receivePacket(Packet packet) {
        endpoint().addInPacket(packet);
    }

    void send();

    static class PacketEndpoint {
        PacketEndpointHost host;

        List<Packet> outPackets = new ArrayList<>();

        LinkedList<Packet> inPackets = new LinkedList<>();

        Packets allInPackets = new Packets();

        private boolean mustSend;

        private PacketBridge bridge;

        public PacketEndpoint(PacketEndpointHost host, PacketBridge bridge) {
            this.host = host;
            this.bridge = bridge;
        }

        public Packet getCorrespondingCommandPacket(Packet packet) {
            return allInPackets.byId(packet.id());
        }

        @Override
        public synchronized String toString() {
            return host.toString();
        }

        synchronized void addInPacket(Packet packet) {
            inPackets.add(packet);
            allInPackets.add(packet);
            bridge.packetsReceived(packet);
        }

        synchronized void addOutPacket(Packet packet) {
            outPackets.add(packet);
        }

        void addReplyPacket(Packet translated) {
            addOutPacket(translated);
        }

        boolean containsResponse(Packet packet) {
            return false;
        }

        synchronized List<Packet> flushOutPackets() {
            List<Packet> flush = outPackets;
            outPackets = new ArrayList<>();
            return flush;
        }

        synchronized Packet next() {
            return inPackets.isEmpty() ? null : inPackets.pop();
        }

        PacketEndpoint otherPacketEndpoint() {
            return bridge.otherPacketEndpoint(this);
        }

        void send() {
            host.send();
        }

        void setMustSend(boolean mustSend) {
            this.mustSend = mustSend;
        }

        synchronized boolean shouldSend() {
            return outPackets.size() > 0;
        }
    }
}
