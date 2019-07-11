package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.Iterator;
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

        List<Packet> inPackets = new ArrayList<>();

        private boolean mustSend;

        public PacketEndpoint(PacketEndpointHost host) {
            this.host = host;
        }

        synchronized void addInPacket(Packet packet) {
            inPackets.add(packet);
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

        // i.e. incoming packets
        Iterator<Packet> packets() {
            return inPackets.iterator();
        }

        void send() {
            host.send();
        }

        void setMustSend(boolean mustSend) {
            this.mustSend = mustSend;
        }

        boolean shouldSend() {
            return outPackets.size() > 0;
        }
    }
}
