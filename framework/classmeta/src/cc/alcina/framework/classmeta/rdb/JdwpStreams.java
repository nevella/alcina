package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

class JdwpStreams implements PacketEndpointHost {
    InputStream fromStream;

    OutputStream toStream;

    protected PacketEndpoint packetEndpoint = new PacketEndpoint(this);

    private RdbEndpointDescriptor descriptor;

    JdwpStreams(RdbEndpointDescriptor descriptor, Socket socket) {
        try {
            this.descriptor = descriptor;
            this.fromStream = socket.getInputStream();
            this.toStream = socket.getOutputStream();
            Ax.out("[%s] : %s >> %s", descriptor.name, fromStream, toStream);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public PacketEndpoint endpoint() {
        return packetEndpoint;
    }

    @Override
    public void send() {
        // TODO Auto-generated method stub
    }

    public void write(Packet packet) {
        try {
            toStream.write(packet.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
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
                        Packet received = new HandshakePacket();
                        received.bytes = handshake;
                        received.fromName = descriptor.name;
                        endpoint().addInPacket(received);
                    }
                    while (true) {
                        byte[] in = new byte[4];
                        fromStream.read(in);
                        int length = Packet.bigEndian(in);
                        if (length > (2 << 18)) {
                            // hack - did we get an out of order handshake
                            // packet?
                            Ax.out("dropping malformed packet");
                            byte[] packet = new byte[10];
                            fromStream.read(packet);
                            return;
                        }
                        byte[] packet = new byte[length - 4];
                        fromStream.read(packet);
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        buffer.write(in);
                        buffer.write(packet);
                        Packet received = new Packet();
                        received.bytes = buffer.toByteArray();
                        received.fromName = descriptor.name;
                        endpoint().addInPacket(received);
                    }
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            };
        }.start();
    }
}