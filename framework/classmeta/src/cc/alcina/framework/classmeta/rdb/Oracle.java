package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;

import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.RdbJdi;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;
import cc.alcina.framework.classmeta.rdb.Packet.Meta;
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

    public void handlePacket(PacketEndpoint packetSource, Packet packet) {
        if (packet.isReply) {
            Packet reply = packet;
            Packet command = packet.getCorrespondingCommandPacket();
            if (command.fromDebugger) {
                if (endpoint.isDebuggee()) {
                    switch (command.meta.series) {
                    case all_threads_handshake: {
                        predict_all_threads_handshake(command, reply);
                        break;
                    }
                    case frames: {
                        if (command.messageName.equals("Frames")) {
                            predict_frames(command, reply);
                        }
                        break;
                    }
                    case variable_table: {
                        if (command.messageName
                                .equals("VariableTableWithGeneric")) {
                            predict_variable_table(command, reply);
                        }
                        break;
                    }
                    }
                }
            }
        }
    }

    public boolean isCacheable(Packet packet) {
        if (packet.messageName == null) {
            // we're not trying to prune objects, just get rid of uncacheable
            // values - if the key (command) is gone, this (reply) is
            // unreachable
            return true;
        }
        switch (packet.messageName) {
        case "Name":
            // valid per frame (which is discarded)
        case "ThisObject":
            // valid for vm lifetime
        case "Signature":
        case "SignatureWithGeneric":
        case "ReferenceType":
        case "LineTable":
        case "SourceDebugExtension":
            return true;
        default:
            return false;
        }
    }

    public void receivedPredictivePacket() {
        if (endpoint.isDebuggee()) {
            synchronized (connection) {
                connection.notifyAll();
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

    private void predict_frames(Packet command, Packet reply) {
        try {
            rdbJdi.predict_frames(command.bytes, reply.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void predict_variable_table(Packet command, Packet reply) {
        try {
            rdbJdi.predict_variable_table(command.bytes, reply.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    void analysePacket(Packet packet) {
        if (endpoint.isDebugger()
                && packet.source == endpoint.streams.packetEndpoint()) {
            state.currentPacket = packet;
            state.updateState();
            if (packet.meta.series == EventSeries.unknown) {
                packet.meta.series = state.currentSeries;
            }
        }
    }

    void onPredictivePacketMiss() {
        Packet currentPacket = state.currentPacket;
        state.setExpectingPredictiveAfter(null);
        state.updateState();
    }

    void preparePacket(PacketEndpoint packetSource, Packet packet) {
        if (packet.meta == null) {
            Meta meta = new Meta();
            meta.mustSend = true;
            packet.meta = meta;
            jwdpAccessor.parse(packet);
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
