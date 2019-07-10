package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

abstract class Transport implements PacketEndpoint {
    protected RdbEndpointDescriptor descriptor;

    public Transport(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    protected void launch() {
    }

    protected abstract void sendPacket(Endpoint from, Packet packet);
}
