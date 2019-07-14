package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;

import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.RdbJdi;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.Packet.Meta;
import cc.alcina.framework.classmeta.rdb.Packet.Type;
import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.WrappedRuntimeException;

class Oracle {
    Accessor jwdpAccessor = new Accessor();

    VirtualMachineImplExt vm;

    DebuggerState state = new DebuggerState();

    RdbJdi rdbJdi;

    private Endpoint endpoint;

    private InternalVmConnection connection;

    public Oracle(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void analysePacket(PacketEndpoint packetSource, Packet packet) {
        if (packet.meta == null) {
            Meta meta = new Meta();
            meta.mustSend = true;
            packet.meta = meta;
            jwdpAccessor.parse(packet);
        }
        if (endpoint.isDebugger()) {
            analyseDebuggerPacket(packet, packet.meta);
        }
    }

    public void handlePacket(PacketEndpoint packetSource, Packet packet) {
        if (packet.isReply) {
            Packet reply = packet;
            Packet command = packet.getCorrespondingCommandPacket();
            switch (command.meta.type) {
            case all_threads_handshake: {
                break;
            }
            }
            if (endpoint.isDebuggee() && command.fromDebugger) {
                switch (command.meta.type) {
                case all_threads_handshake: {
                    predict_all_threads_handshake(command, reply);
                    int debug = 3;
                    break;
                }
                }
            }
        }
    }

    public void receivedPredictivePacket() {
        if (endpoint.isDebuggee()) {
            synchronized (connection) {
                connection.notifyAll();
            }
        }
    }

    private void analyseDebuggerPacket(Packet packet, Meta meta) {
        switch (packet.messageName) {
        case "AllThreads": {
            if (!state.calledAllThreads) {
                state.calledAllThreads = true;
                meta.type = Type.all_threads_handshake;
            }
        }
        }
        if (meta.type == Type.unknown) {
            if (!state.calledAllThreads) {
                meta.type = Type.early_handshake;
            } else {
                meta.type = Type.unknown_post_handshake;
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

    void start() {
        if (endpoint.isDebuggee()) {
            connection = new InternalVmConnection();
        }
        vm = new VirtualMachineImplExt(connection);
        rdbJdi = new RdbJdi(vm);
    }

    class InternalVmConnection extends Connection {
        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public byte[] readPacket() throws IOException {
            while (true) {
                Packet packet = streams().packetEndpoint()
                        .nextIncomingPredictivePacket();
                if (packet != null) {
                    packet = streams().translateToOriginalId(packet);
                    return packet.bytes;
                }
                synchronized (connection) {
                    try {
                        connection.wait();
                    } catch (Exception e) {
                        throw new WrappedRuntimeException(e);
                    }
                }
            }
        }

        @Override
        public void writePacket(byte[] pkt) throws IOException {
            Packet packet = new Packet(null);
            packet.bytes = pkt;
            packet.isPredictive = true;
            jwdpAccessor.parse(packet);
            streams().write(packet);
        }

        JdwpStreams streams() {
            return endpoint.streams;
        }
    }
}
