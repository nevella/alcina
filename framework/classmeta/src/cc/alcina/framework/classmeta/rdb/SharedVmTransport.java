package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

class SharedVmTransport extends Transport {
    // private Thread receiver;
    Endpoint from;

    Endpoint to;

    public SharedVmTransport(RdbEndpointDescriptor descriptor) {
        super(descriptor);
        from = RdbProxies.get().endpointByName(descriptor.name);
        to = RdbProxies.get().endpointByName(descriptor.transportEndpointName);
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
    protected void sendPacket(Endpoint from, Packet packet) {
        Endpoint other = from == this.from ? to : this.from;
        other.onPacketFromEndpoint(packet);
    }
}
