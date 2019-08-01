package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.util.List;

import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.RdbJdi;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;
import cc.alcina.framework.classmeta.rdb.Packet.Meta;
import cc.alcina.framework.classmeta.rdb.Packet.PacketPair;
import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

class Oracle {
    Accessor jwdpAccessor = new Accessor();

    VirtualMachineImplExt vm;

    DebuggerState state = new DebuggerState();

    RdbJdi rdbJdi;

    private Endpoint endpoint;

    private InternalVmConnection connection;

    EventSeries predictForSeries = null;

    public Oracle(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Packet createAckPacket(Packet packet) {
        // TODO Auto-generated method stub
        return null;
    }

    public void handlePacket(PacketEndpoint packetSource, Packet packet) {
        if (packet.isReply) {
            Packet reply = packet;
            Packet command = packet.getCorrespondingCommandPacket();
            if (command.fromDebugger) {
                if (endpoint.isDebuggee()) {
                    predictForSeries = command.meta.series;
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
                    case get_values_stack_frame: {
                        if (command.messageName.equals("GetValues")) {
                            predict_get_values_stack_frame(command, reply);
                        }
                        break;
                    }
                    }
                }
            }
        } else {
            Packet command = packet;
            if (endpoint.isDebuggee()) {
                if (command.messageName.equals("Composite")) {
                    send_composite_reply(command);
                } else if (command.messageName.equals("IsCollected")) {
                    debug_is_collected(command);
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
        // can be forcibly invalidated if need be
        case "Name":
            // valid for the frame lifetime (and the frame is discarded)
        case "ThisObject":
            // valid for vm lifetime
        case "ThreadGroup":
        case "Signature":
        case "SignatureWithGeneric":
        case "ClassesBySignature":
        case "ReferenceType":
        case "MethodsWithGeneric":
        case "LineTable":
        case "SourceDebugExtension":
        case "SourceFile":
        case "FieldsWithGeneric":
        case "Modifiers":
        case "Superclass":
        case "Interfaces":
            return true;
        case "Set":
            return false;
        case "GetValues":
            // not after any invalidation, unless early in the allthreads phase
            if (packet.predictiveFor == EventSeries.admin_post_handshake
                    && !state.seenSuspend) {
                return true;
            } else {
                return false;
            }
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

    private void debug_is_collected(Packet command) {
        try {
            rdbJdi.debug_is_collected(command.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
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

    private void predict_get_values_stack_frame(Packet command, Packet reply) {
        try {
            rdbJdi.predict_get_values_stack_frame(command.bytes, reply.bytes);
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

    private void send_composite_reply(Packet command) {
        try {
            rdbJdi.send_composite_reply(command.bytes);
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

    void beforePacketMiss(Packet packet) {
        switch (packet.messageName) {
        case "Suspend":
        case "VariableTableWithGeneric":
        case "FrameCount":
            // cool, not an unforced miss
            return;
        // case "Signature":
        // // not cool but livable
        // return;
        }
        switch (state.currentSeries) {
        case frames:
        case variable_table:
        case get_values_stack_frame:
            List<Packet> hits = packet.source.otherPacketEndpoint()
                    .currentPredictivePacketsHit();
            List<PacketPair> like = packet.source.otherPacketEndpoint()
                    .getPredictivePacketsLike(packet);
            List<PacketPair> last = packet.source.otherPacketEndpoint()
                    .getMostRecentPredictivePacketList();
            Ax.out("***hits***");
            Ax.out(hits);
            Ax.out("\n***like***");
            Ax.out(like);
            Ax.out("\n***last***");
            Ax.out(last);
            Ax.out("");
            if (packet.messageName.equals("ThisObject")) {
                rdbJdi.debugThisObject(packet.bytes);
            }
            int debug = 3;
            break;
        }
    }

    void onPredictivePacketMiss() {
        Packet currentPacket = state.currentPacket;
        state.setExpectingPredictiveAfter(null);
        state.updateState();
    }

    void preparePacket(Packet packet) {
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
            packet.predictiveFor = predictForSeries;
            jwdpAccessor.parse(packet);
            streams().write(packet);
        }

        JdwpStreams streams() {
            return endpoint.streams;
        }
    }
}
