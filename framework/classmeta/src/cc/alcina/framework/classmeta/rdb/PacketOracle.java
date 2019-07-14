package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;

import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.RdbJdi;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.WrappedRuntimeException;

class PacketOracle {
    Accessor jwdpAccessor = new Accessor();

    VirtualMachineImplExt vm;

    DebuggerState state = new DebuggerState();

    RdbJdi rdbJdi;

    public PacketOracle() {
        InternalVmConnection connection = new InternalVmConnection();
        vm = new VirtualMachineImplExt(connection);
        rdbJdi = new RdbJdi(vm);
    }

    public void analysePacket(Endpoint endpoint, PacketEndpoint packetSource,
            Packet packet) {
        if (packet.meta == null) {
            PacketMeta meta = new PacketMeta();
            meta.mustSend = true;
            packet.meta = meta;
            jwdpAccessor.parse(packet);
        }
        if (endpoint.isDebugger()) {
            analyseDebuggerPacket(packet, packet.meta);
        }
    }

    public void handlePacket(Endpoint endpoint, PacketEndpoint packetSource,
            Packet packet) {
        if (packet.isReply) {
            Packet reply = packet;
            Packet command = packet.getCorrespondingCommandPacket();
            switch (command.meta.type) {
            case all_threads_handshake: {
                int debug = 3;
                break;
            }
            }
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
        if (meta.type == PacketType.unknown) {
            if (!state.calledAllThreads) {
                meta.type = PacketType.early_handshake;
            } else {
                meta.type = PacketType.unknown_post_handshake;
            }
        }
    }

    private void predict_all_threads_handshake(Packet command, Packet reply) {
        try {
            rdbJdi.predict_all_threads_handshake(command.bytes, reply.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    class InternalVmConnection extends Connection {
        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean isOpen() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public byte[] readPacket() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void writePacket(byte[] pkt) throws IOException {
            // TODO Auto-generated method stub
        }
    }
}
