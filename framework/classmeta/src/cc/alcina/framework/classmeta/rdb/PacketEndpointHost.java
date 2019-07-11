package cc.alcina.framework.classmeta.rdb;

import java.util.Iterator;

interface PacketEndpointHost {
    PacketEndpoint endpoint();

    static class PacketEndpoint {
        PacketEndpointHost host;

        public PacketEndpoint(PacketEndpointHost host) {
            this.host = host;
        }

        void addOutPacket(Packet packet) {
            throw new UnsupportedOperationException();
        }

        void addReplyPacket(Packet translated) {
            throw new UnsupportedOperationException();
        }

        boolean containsResponse(Packet packet) {
            throw new UnsupportedOperationException();
        }

        Iterator<Packet> packets() {
            return null;
        }

        void send() {
            throw new UnsupportedOperationException();
        }

        void setMustSend(boolean b) {
            throw new UnsupportedOperationException();
        }

        boolean shouldSend() {
            throw new UnsupportedOperationException();
        }
    }
}
