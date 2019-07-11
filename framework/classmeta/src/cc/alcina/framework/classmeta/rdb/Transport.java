package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

abstract class Transport implements PacketEndpointHost {
    protected RdbEndpointDescriptor descriptor;

    protected PacketEndpoint packetEndpoint = new PacketEndpoint(this);

    public Transport(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public PacketEndpoint endpoint() {
        return packetEndpoint;
    }

    protected void launch() {
    }

    protected abstract void sendPacket(Endpoint from, Packet packet);
}
