package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;
//unused
class RdbHandshakeDebugeePlayer {
    RdbPackets handshakePackets;

    int packetCursor = 0;

    int packetCount = 0;

    Socket internalDebuggerSocket;

    Logger logger = LoggerFactory.getLogger(getClass());

    public RdbHandshakeDebugeePlayer(Socket internalDebuggerSocket) {
        this.internalDebuggerSocket = internalDebuggerSocket;
    }

    void loadHandshakePackets() {
        handshakePackets = JacksonUtils.deserializeNoTypes(
                ResourceUtilities.readClazzp("handshake-packets.json"),
                RdbPackets.class);
    }

    void play() throws IOException {
        loadHandshakePackets();
        // handshake
        {
            RdbPacket fromInternal = handshakePackets.packets
                    .get(packetCursor++);
            RdbPacket toInternal = handshakePackets.packets.get(packetCursor++);
            byte[] in = new byte[fromInternal.bytes.length];
            internalDebuggerSocket.getInputStream().read(in);
            logger.info("Internal debugger :: >>> {} bytes", in.length);
            internalDebuggerSocket.getOutputStream().write(toInternal.bytes);
            logger.info("Internal debugger :: <<< {} bytes",
                    toInternal.bytes.length);
        }
        while (++packetCount < 4) {
            byte[] in = new byte[4];
            internalDebuggerSocket.getInputStream().read(in);
            int length = RdbPacket.bigEndian(in);
            byte[] packet = new byte[length];
            internalDebuggerSocket.getInputStream().read(packet);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(in);
            buffer.write(packet);
            RdbPacket receivedFromInternal = new RdbPacket();
            receivedFromInternal.bytes = buffer.toByteArray();
            logger.info("Internal debugger :: >>> {} ", receivedFromInternal);
            Optional<RdbPacket> o_equivalentFromHandshake = handshakePackets
                    .find(receivedFromInternal.commandSet(),
                            receivedFromInternal.commandId());
            RdbPacket equivalentFromHandshake = null;
            RdbPacket toInternalReply = null;
            equivalentFromHandshake = o_equivalentFromHandshake.get();
            RdbPacket handshakeReply = handshakePackets
                    .findReply(equivalentFromHandshake.id());
            toInternalReply = handshakeReply.copy();
            toInternalReply.fromName = "(to internal copy)"
                    + toInternalReply.fromName;
            toInternalReply.setId(receivedFromInternal.id());
            logger.info("Internal debugger :: <<< {} ", toInternalReply);
            internalDebuggerSocket.getOutputStream()
                    .write(toInternalReply.bytes);
        }
    }
}