package cc.alcina.framework.classmeta.rdb;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.util.Ax;

abstract class Transport implements PacketEndpointHost {
    protected RdbEndpointDescriptor descriptor;

    protected PacketEndpoint packetEndpoint;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public Transport(RdbEndpointDescriptor descriptor, Endpoint endpoint) {
        this.descriptor = descriptor;
        this.packetEndpoint = new PacketEndpoint(this, endpoint);
    }

    @Override
    public PacketEndpoint packetEndpoint() {
        return packetEndpoint;
    }

    @Override
    public String toString() {
        return Ax.format("%s::%s", getClass().getSimpleName(), descriptor.name);
    }

    protected void launch() {
    }

    protected void receivePredictivePackets(List<Packet> predictivePackets) {
        packetEndpoint.receivedPredictivePackets(predictivePackets);
    }
}
