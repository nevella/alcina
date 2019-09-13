package cc.alcina.framework.classmeta.rdb;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

class Packet {
    static boolean debugPredictivePassthrough = false;

    public static int bigEndian(byte[] byteArray) {
        return bigEndian(byteArray[0], byteArray[1], byteArray[2],
                byteArray[3]);
    }

    static int bigEndian(byte b1, byte b2, byte b3, byte b4) {
        return (b1 << 24) + (b2 << 16 & 0xFFFFFF) + (b3 << 8 & 0xFFFF)
                + (b4 & 0xFF);
    }

    public byte[] bytes = new byte[11];

    boolean fromDebugger;

    boolean isPredictive;

    boolean notifySuspended;

    public String fromName;

    public String messageName;

    /*
     * FIXME - may not need this. May handle invalidation differently
     */
    EventSeries predictiveFor;

    transient Message message;

    transient PacketEndpoint source;

    transient PacketPayload payload;

    public boolean isReply;

    transient int predictivePacketUseCount;

    boolean mustSend;

    EventSeries series = EventSeries.unknown;

    boolean preparedByOracle;

    public long suspendId;

    public Packet() {
    }

    public Packet(PacketEndpoint source) {
        this.source = source;
    }

    public Packet copy() {
        Packet copy = ResourceUtilities.fieldwiseClone(this);
        copy.bytes = Arrays.copyOf(bytes, bytes.length);
        return copy;
    }

    public void dump() {
        String dir = fromDebugger ? ">>>" : "<<<";
        Ax.out("%s : %s %s\n", fromName, dir, toString());
        SEUtilities.dumpBytes(bytes, 11);
    }

    @JsonIgnore
    public Packet getCorrespondingCommandPacket() {
        Preconditions.checkNotNull(source,
                "can only be applied to packets generated/streamed from this vm");
        return source.otherPacketEndpoint().getCorrespondingCommandPacket(this);
    }

    @JsonIgnore
    public void setId(int id) {
        bytes[4] = (byte) ((id >> 24) & 0xFF);
        bytes[5] = (byte) ((id >> 16) & 0xFF);
        bytes[6] = (byte) ((id >> 8) & 0xFF);
        bytes[7] = (byte) ((id) & 0xFF);
    }

    @Override
    public String toString() {
        String predictiveMarker = isPredictive ? "(predictive)"
                : "(passthrough)";
        if (!debugPredictivePassthrough) {
            predictiveMarker = "";
        }
        String seriesMarker = series.toString();
        if (!fromDebugger || isReply) {
            seriesMarker = "";
        }
        return Ax.format("%s/%s/%s\t%s\t%s\t%s", id(), commandSet(),
                commandId(), Ax.blankToEmpty(messageName), predictiveMarker,
                seriesMarker);
    }

    int commandId() {
        return bytes[10];
    }

    int commandSet() {
        return bytes[9];
    }

    int flags() {
        return bytes[8];
    }

    int id() {
        return bigEndian(bytes[4], bytes[5], bytes[6], bytes[7]);
    }

    int length() {
        return bigEndian(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    PacketPayload payload() {
        if (payload == null) {
            payload = new PacketPayload();
        }
        return payload;
    }

    enum EventSeries {
        early_handshake, all_threads_handshake, unknown_post_handshake, unknown,
        admin_post_handshake, breakpoint_set, contended_monitor_check, suspend,
        frames, variable_table, get_values_stack_frame,
        get_values_reference_type, get_values_object_reference,
        get_values_array_reference
    }

    static class HandshakePacket extends Packet {
        public HandshakePacket() {
        }

        public HandshakePacket(PacketEndpoint source) {
            super(source);
        }

        @Override
        public String toString() {
            return "JDWP-Handshake";
        }
    }

    static class PacketPair {
        Packet command;

        Packet reply;

        public PacketPair(Packet command, Packet reply) {
            this.command = command;
            this.reply = reply;
        }

        @Override
        public String toString() {
            return Ax.format("(pair) :: %s", command);
        }
    }

    class PacketPayload {
        int hash = -1;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PacketPayload) {
                return Arrays.equals(zeroIdCopy().bytes,
                        ((PacketPayload) obj).zeroIdCopy().bytes);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            if (hash == -1) {
                Packet copy = zeroIdCopy();
                hash = Arrays.hashCode(copy.bytes);
            }
            return hash;
        }

        private Packet zeroIdCopy() {
            Packet copy = copy();
            copy.setId(0);
            return copy;
        }
    }
}
