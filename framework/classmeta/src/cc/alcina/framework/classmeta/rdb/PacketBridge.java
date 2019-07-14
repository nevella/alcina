package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;

interface PacketBridge {
    PacketEndpoint otherPacketEndpoint(PacketEndpoint packetEndpoint);

    void packetsReceived(Packet packet);
}