package cc.alcina.framework.classmeta.rdb;

import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;

class PacketCategories {
    Accessor jwdpAccessor = new Accessor();

    VirtualMachineImplExt vm = new VirtualMachineImplExt();

    DebuggerState state = new DebuggerState();

    public void analysePacket(Endpoint endpoint, PacketEndpoint packetSource,
            Packet packet) {
        PacketMeta meta = new PacketMeta();
        meta.mustSend = true;
        packet.meta = meta;
        jwdpAccessor.parse(packet);
        if (endpoint.isDebugger()) {
            analyseDebuggerPacket(packet, meta);
        }
    }

    public void handlePacket(Endpoint endpoint, PacketEndpoint packetSource,
            Packet packet) {
        if (packet.isReply) {
            Packet reply = packet;
            Packet command = packet.getCorrespondingCommandPacket();
            if (endpoint.isDebuggee() && command.fromDebugger) {
                switch (command.meta.type) {
                case all_threads_handshake: {
                    predict_all_threads_handshake(command, reply);
                    break;
                }
                }
            }
        }
    }

    private void analyseDebuggerPacket(Packet packet, PacketMeta meta) {
        switch (packet.messageName) {
        case "AllThreads": {
            if (!state.calledAllThreads) {
                state.calledAllThreads = true;
                meta.type = PacketType.all_threads_handshake;
            }
        }
        }
        if (meta.type == null) {
            if (!state.calledAllThreads) {
                meta.type = PacketType.early_handshake;
            } else {
                meta.type = PacketType.unknown_post_handshake;
            }
        }
    }

    private void predict_all_threads_handshake(Packet command, Packet reply) {
        // TODO Auto-generated method stub
    }
}
