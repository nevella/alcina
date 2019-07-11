package cc.alcina.framework.classmeta.rdb;

import java.util.Arrays;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

class Packet {
    public static int bigEndian(byte[] byteArray) {
        return bigEndian(byteArray[0], byteArray[1], byteArray[2],
                byteArray[3]);
    }

    private static int un2c(byte b) {
        return b < 0 ? b + 256 : b;
    }

    static int bigEndian(byte b1, byte b2, byte b3, byte b4) {
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + un2c(b4);
    }

    public byte[] bytes = new byte[11];

    public boolean fromDebugger;

    public String fromName;

    public String messageName;

    transient Message message;

    transient PacketEndpoint source;

    transient PacketMeta meta;

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

    public void setId(int id) {
        bytes[4] = (byte) ((id >> 24) & 0xFF);
        bytes[5] = (byte) ((id >> 16) & 0xFF);
        bytes[6] = (byte) ((id >> 8) & 0xFF);
        bytes[7] = (byte) ((id) & 0xFF);
    }

    @Override
    public String toString() {
        return Ax.format("%s/%s/%s %s", id(), commandSet(), commandId(),
                Ax.blankToEmpty(messageName));
    }

    public Packet translate(PacketEndpoint otherSource,
            PacketEndpoint packetSource) {
        // TODO Auto-generated method stub
        return null;
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
}
