package cc.alcina.framework.classmeta.rdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.util.Ax;

abstract class Transport implements PacketEndpointHost {
    protected RdbEndpointDescriptor descriptor;

    protected PacketEndpoint packetEndpoint;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public Transport(RdbEndpointDescriptor descriptor,
            PacketBridge bridge) {
        this.descriptor = descriptor;
        this.packetEndpoint = new PacketEndpoint(this, bridge);
    }

    @Override
    public PacketEndpoint endpoint() {
        return packetEndpoint;
    }

    @Override
    public String toString() {
        return Ax.format("%s::%s", getClass().getSimpleName(), descriptor.name);
    }

    protected void launch() {
    }

    protected abstract void sendPacket(Packet packet);
}
