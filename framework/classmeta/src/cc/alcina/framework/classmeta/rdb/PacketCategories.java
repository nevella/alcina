package cc.alcina.framework.classmeta.rdb;

import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;

class PacketCategories {
    Accessor jwdpAccessor = new Accessor();

    protected VirtualMachineImplExt vm = new VirtualMachineImplExt();

    public void analysePacket(Endpoint endpoint, PacketEndpoint packetSource,
            Packet packet) {
        PacketMeta meta = new PacketMeta();
        meta.mustSend = true;
        packet.meta = meta;
        jwdpAccessor.parse(packet);
    }
}
