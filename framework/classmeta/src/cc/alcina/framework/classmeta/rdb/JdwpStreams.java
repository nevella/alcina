package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.classmeta.rdb.Packet.HandshakePacket;
import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

class JdwpStreams implements PacketEndpointHost {
    InputStream fromStream;

    OutputStream toStream;

    protected PacketEndpoint packetEndpoint;

    private RdbEndpointDescriptor descriptor;

    Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean debugRead;

    private Endpoint endpoint;

    boolean closed;

    JdwpStreams(RdbEndpointDescriptor descriptor, Socket socket,
            Endpoint endpoint) {
        this.endpoint = endpoint;
        try {
            this.descriptor = descriptor;
            this.fromStream = socket.getInputStream();
            this.toStream = socket.getOutputStream();
            this.packetEndpoint = new PacketEndpoint(this, endpoint);
            Ax.out("[%s] : %s >> %s", descriptor.name, fromStream, toStream);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public void addPredictivePackets(List<Packet> predictivePackets) {
        if (predictivePackets.isEmpty()) {
            return;
        }
        throw new UnsupportedOperationException();
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public PacketEndpoint packetEndpoint() {
        return packetEndpoint;
    }

    @Override
    public void send() {
        List<Packet> packets = packetEndpoint().flushOutPackets();
        for (Packet packet : packets) {
            write(packet);
        }
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public String toString() {
        return Ax.format("%s::%s", getClass().getSimpleName(), descriptor.name);
    }

    public Packet translateToOriginalId(Packet packet) {
        return packetEndpoint.translateToOriginalId(packet);
    }

    void start() {
        new Thread(Ax.format("%s::jdwp-reader", descriptor.name)) {
            @Override
            public void run() {
                try {
                    {
                        byte[] handshake = new byte[14];
                        // JWDP-Handshake
                        fromStream.read(handshake);
                        Packet received = new HandshakePacket(packetEndpoint);
                        received.bytes = handshake;
                        received.fromName = descriptor.name;
                        logger.info("Received handshake << {}",
                                descriptor.name);
                        packetEndpoint().addInPacket(received);
                    }
                    while (true) {
                        if (isClosed()) {
                            return;
                        }
                        int b1, b2, b3, b4;
                        // length
                        b1 = fromStream.read();
                        b2 = fromStream.read();
                        b3 = fromStream.read();
                        b4 = fromStream.read();
                        // EOF
                        if (b1 < 0) {
                            break;
                        }
                        if (b2 < 0 || b3 < 0 || b4 < 0) {
                            throw new IOException(
                                    "protocol error - premature EOF");
                        }
                        int len = ((b1 << 24) | (b2 << 16) | (b3 << 8)
                                | (b4 << 0));
                        if (len < 0) {
                            throw new IOException(
                                    "protocol error - invalid length");
                        }
                        byte b[] = new byte[len];
                        b[0] = (byte) b1;
                        b[1] = (byte) b2;
                        b[2] = (byte) b3;
                        b[3] = (byte) b4;
                        int off = 4;
                        len -= off;
                        while (len > 0) {
                            int count;
                            count = fromStream.read(b, off, len);
                            if (count < 0) {
                                throw new IOException(
                                        "protocol error - premature EOF");
                            }
                            len -= count;
                            off += count;
                        }
                        Packet packet = new Packet(packetEndpoint);
                        packet.bytes = b;
                        packet.fromName = descriptor.name;
                        packetEndpoint().addInPacket(packet);
                    }
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                } finally {
                    packetEndpoint().close();
                }
            };
        }.start();
    }

    synchronized void write(Packet packet) {
        try {
            if (packet.isPredictive) {
                if (endpoint.isDebuggee()) {
                    packet = packetEndpoint.createForPredictiveSequence(packet);
                }
                logger.debug("Send packet :: {}\n\t{}", packetEndpoint, packet);
                // debugRead = true;
            } else {
                if (packet.isReply) {
                    // end of stanza
                    Ax.out("");
                }
            }
            toStream.write(packet.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}