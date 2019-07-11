package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cc.alcina.framework.classmeta.rdb.RdbProxy.StreamListener;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

class JdwpStreams implements PacketEndpointHost {
    String name;

    InputStream fromStream;

    OutputStream toStream;

    StreamListener streamListener;

    JdwpStreams(String name, StreamListener streamListener,
            InputStream inputStream, OutputStream outputStream) {
        this.name = name;
        this.streamListener = streamListener;
        this.fromStream = inputStream;
        this.toStream = outputStream;
        Ax.out("[%s] : %s >> %s", name, fromStream, toStream);
    }

    @Override
    public PacketEndpoint endpoint() {
        // TODO Auto-generated method stub
        return null;
    }

    public void write(Packet packet) {
        try {
            toStream.write(packet.bytes);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    void start() {
        byte[] handshake = new byte[14];
        try {
            // JWDP-Handshake
            fromStream.read(handshake);
            toStream.write(handshake);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        new Thread(name) {
            @Override
            public void run() {
                try {
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
                        received.fromName = name;
                        streamListener.packetReceived(JdwpStreams.this,
                                received);
                    }
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }
        }.start();
    }
}