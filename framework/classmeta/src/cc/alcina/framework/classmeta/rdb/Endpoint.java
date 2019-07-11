package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

@SuppressWarnings("resource")
abstract class Endpoint {
    protected Transport transport;

    protected JdwpStreams streams;

    protected RdbEndpointDescriptor descriptor;

    protected VirtualMachineImplExt vm = new VirtualMachineImplExt();

    PacketCategories categories = new PacketCategories();

    private Object trafficMonitor = new Object();

    Logger logger = LoggerFactory.getLogger(getClass());

    Socket socket = null;

    public Endpoint(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void onPacketFromEndpoint(Packet packet) {
        // TODO Auto-generated method stub
    }

    private void doAttachJdwp() {
        try {
            socket = new Socket(descriptor.jdwpHost, descriptor.jdwpPort);
            logger.info("JDWP attach - {}", descriptor.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inPackets(PacketEndpoint packetEndpoint) {
        PacketEndpoint otherEndpoint = otherEndpoint(packetEndpoint);
        Iterator<Packet> packets = packetEndpoint.packets();
        while (packets.hasNext()) {
            // this is the naive (raw) packet from the endpoint.
            Packet packet = packets.next();
            // what's our coordinate space, Scotty?
            categories.analysePacket(this, packetEndpoint, packet);
            if (otherEndpoint.containsResponse(packet)) {
                Packet translated = packet.translate(otherEndpoint,
                        packetEndpoint);
                packetEndpoint.addReplyPacket(translated);
                continue;
            }
            packetEndpoint.addOutPacket(packet);
            if (packet.meta.mustSend) {
                packetEndpoint.setMustSend(true);
                break;
            }
        }
    }

    private void listenForJdwpAttach() {
        new Thread(Ax.format("%s::jdwp-attach", descriptor.name)) {
            @Override
            public void run() {
                try {
                    listenForJdwpAttach0();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void listenForJdwpAttach0() throws IOException, Exception {
        ServerSocket serverSocket = new ServerSocket(descriptor.jdwpPort);
        while (true) {
            try {
                Socket newSocket = serverSocket.accept();
                if (socket != null) {
                    throw new IOException(
                            "Only one simultaneous connection allowed");
                }
                socket = newSocket;
                logger.info("JDWP attach - {}", descriptor.name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private PacketEndpoint otherEndpoint(PacketEndpoint packetEndpoint) {
        return packetEndpoint.host == streams ? transport.endpoint()
                : streams.endpoint();
    }

    private void outPackets(PacketEndpoint packetEndpoint) {
        if (packetEndpoint.shouldSend()) {
            packetEndpoint.send();
        }
    }

    private void startMainLoop() throws Exception {
        new Thread(Ax.format("%s::main", descriptor.name)) {
            @Override
            public void run() {
                try {
                    while (true) {
                        synchronized (trafficMonitor) {
                            trafficMonitor.wait();
                            inPackets(streams.endpoint());
                            inPackets(transport.endpoint());
                            outPackets(streams.endpoint());
                            outPackets(transport.endpoint());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void startTransportStream() {
        transport = new SharedVmTransport(descriptor);
        transport.launch();
        // FIXME - e.g. debuggee sharedvm - this
    }

    protected void startJdwpStream() {
        if (descriptor.jdwpAttach) {
            doAttachJdwp();
        } else {
            listenForJdwpAttach();
        }
    }

    void launch() {
        startJdwpStream();
        startTransportStream();
        try {
            startMainLoop();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
