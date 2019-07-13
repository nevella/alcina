package cc.alcina.framework.classmeta.rdb;

import java.util.List;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

class SharedVmTransport extends Transport {
    public SharedVmTransport(RdbEndpointDescriptor descriptor,
            PacketListener listener) {
        super(descriptor, listener);
    }

    @Override
    public void send() {
        List<Packet> packets = endpoint().flushOutPackets();
        for (Packet packet : packets) {
            sendPacket(packet);
        }
    }

    private Endpoint to() {
        return RdbProxies.get()
                .endpointByName(descriptor.transportEndpointName);
    }

    @Override
    protected void launch() {
        // no need, other endpoint main
        // receiver = new Thread(Ax.format("%s::transport::receiver",
        // descriptor.name)) {
        // @Override
        // public void run() {
        // transport.run();
        // }
        // };
        // receiver.start();
    }

    @Override
    protected void sendPacket(Packet packet) {
        Endpoint other = to();
        logger.debug("Send packet :: {}\n\t{}", packetEndpoint, packet);
        other.transport.receivePacket(packet);
    }
}
