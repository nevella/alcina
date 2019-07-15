package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
                    byte[] handshakeFirstFour = "JDWP"
                            .getBytes(StandardCharsets.UTF_8);
                    while (true) {
                        byte[] in = new byte[4];
                        int bytesRead = fromStream.read(in);
                        if (bytesRead == -1) {
                            // stream closed
                            // TODO - fire event
                            return;
                        }
                        if (debugRead) {
                            int debug = 3;
                        }
                        if (Arrays.equals(in, handshakeFirstFour)) {
                            byte[] handshake = new byte[14];
                            // JWDP-Handshake
                            fromStream.read(handshake);
                            Packet received = new HandshakePacket(
                                    packetEndpoint);
                            received.bytes = handshake;
                            received.fromName = descriptor.name;
                            logger.info("Received handshake << {}",
                                    descriptor.name);
                            packetEndpoint().addInPacket(received);
                            continue;
                        }
                        int length = Packet.bigEndian(in);
                        if (length > (2 << 18)) {
                            // hack - did we get an out of order handshake
                            // packet?
                            Ax.out("received malformed packet :: panic");
                            return;
                        }
                        byte[] packet = new byte[length - 4];
                        fromStream.read(packet);
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        buffer.write(in);
                        buffer.write(packet);
                        Packet received = new Packet(packetEndpoint);
                        received.bytes = buffer.toByteArray();
                        received.fromName = descriptor.name;
                        packetEndpoint().addInPacket(received);
                    }
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            };
        }.start();
    }

    synchronized void write(Packet packet) {
        try {
            logger.debug("Send packet :: {}\n\t{}", packetEndpoint, packet);
            if (packet.isPredictive) {
                if (endpoint.isDebuggee()) {
                    packet = packetEndpoint.createForPredictiveSequence(packet);
                }
                logger.debug("Send packet :: {}\n\t{}", packetEndpoint, packet);
                // debugRead = true;
            }
            toStream.write(packet.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}