package cc.alcina.framework.classmeta.rdb;

import java.util.List;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

class SharedVmTransport extends Transport {
    public SharedVmTransport(RdbEndpointDescriptor descriptor,
            Endpoint endpoint) {
        super(descriptor, endpoint);
    }

    @Override
    public void addPredictivePackets(List<Packet> predictivePackets) {
        if (predictivePackets.isEmpty()) {
            return;
        }
        to().transport.receivePredictivePackets(predictivePackets);
    }

    @Override
    public void send() {
        List<Packet> packets = packetEndpoint().flushOutPackets();
        for (Packet packet : packets) {
            sendPacket(packet);
        }
    }

    private void sendPacket(Packet packet) {
        Endpoint other = to();
        logger.debug("Send packet :: {}\n\t{}", packetEndpoint, packet);
        other.transport.receivePacket(packet);
    }

    private Endpoint to() {
        return RdbProxies.get()
                .endpointByName(descriptor.transportEndpointName);
    }

    @Override
    protected void launch() {
        // no need, other endpoint main
    }

    @Override
    void close() {
    }
}
