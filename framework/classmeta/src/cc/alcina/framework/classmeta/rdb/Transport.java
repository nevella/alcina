package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

abstract class Transport implements PacketEndpointHost {
    protected RdbEndpointDescriptor descriptor;

    public Transport(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public PacketEndpoint endpoint() {
        // TODO Auto-generated method stub
        return null;
    }

    protected void launch() {
    }

    protected abstract void sendPacket(Endpoint from, Packet packet);
}
