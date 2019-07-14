package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

@SuppressWarnings("resource")
/*
 * Endpoint vs PacketEndpoint
 * 
 * The process has two endpoints (debugger, debuggee) represented by two
 * Endpoint instances (either same jvm or different)
 * 
 * Each endpoint has two packetendpoints (that send and recevie packets):
 * 
 * transport (communicate with other endpoint)
 * 
 * streams (communicate via jdwp with debugger/debuggee jvms)
 * 
 * 
 */
abstract class Endpoint {
    protected Transport transport;

    protected JdwpStreams streams;

    protected RdbEndpointDescriptor descriptor;

    Oracle oracle = new Oracle(this);

    Logger logger = LoggerFactory.getLogger(getClass());

    Socket socket = null;

    AtomicInteger eventCounter = new AtomicInteger(0);

    int inPacketCounter = 0;

    int predictiveReplyPacketCounter = 0;

    public Endpoint(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PacketEndpoint otherPacketEndpoint(PacketEndpoint packetEndpoint) {
        return packetEndpoint.host == streams ? transport.packetEndpoint()
                : streams.packetEndpoint();
    }

    public void packetsReceived(Packet packet) {
        if (!packet.fromDebugger) {
            packet.fromDebugger = this.isDebugger()
                    && packet.source.host == streams;
        }
        if (packet.isPredictive) {
            oracle.receivedPredictivePacket();
        } else {
            synchronized (eventCounter) {
                eventCounter.incrementAndGet();
                eventCounter.notify();
            }
        }
    }

    private void doAttachJdwp() {
        try {
            socket = new Socket(descriptor.jdwpHost, descriptor.jdwpPort);
            startSocketInterceptor();
            logger.info("JDWP attached >> {}", descriptor.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inPackets(PacketEndpoint packetEndpoint) {
        PacketEndpoint otherPacketEndpoint = otherPacketEndpoint(
                packetEndpoint);
        Packet packet = null;
        // this is the naive (raw) packet from the endpoint.
        while ((packet = packetEndpoint.next()) != null) {
            // what's our coordinate space, Scotty?
            if (packetEndpoint.host == streams) {
                inPacketCounter++;
                logger.info("Packets: {}/{}", predictiveReplyPacketCounter,
                        inPacketCounter);
            }
            oracle.analysePacket(packetEndpoint, packet);
            oracle.handlePacket(packetEndpoint, packet);
            if (packetEndpoint.host instanceof SharedVmTransport) {
            } else {
                logger.info("Received packet :: {}\t{}", packetEndpoint,
                        packet);
            }
            Optional<Packet> predictiveResponse = otherPacketEndpoint
                    .getPredictiveResponse(packet);
            if (predictiveResponse.isPresent()) {
                packetEndpoint.addReplyPacket(predictiveResponse.get());
                predictiveReplyPacketCounter++;
                break;
            }
            otherPacketEndpoint.addOutPacket(packet);
            otherPacketEndpoint.host.addPredictivePackets(
                    packetEndpoint.flushPredictivePackets());
            if (packet.meta.mustSend) {
                otherPacketEndpoint.setMustSend(true);
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
                startSocketInterceptor();
                logger.info("JDWP attached << {}", descriptor.name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                    int eventsProcessed = 0;
                    while (true) {
                        int eventsAtStartOfLoop = 0;
                        synchronized (eventCounter) {
                            // the main thing is to not miss wakeups - so if an
                            // incoming packet is received on another thread
                            // during this loop (and the counter is incremented
                            // outside this sync block) - loop will rerun
                            //
                            // who knows - there may be a .concurrent class that
                            // does this too
                            eventsAtStartOfLoop = eventCounter.get();
                            if (eventsAtStartOfLoop == eventsProcessed) {
                                eventCounter.wait();
                            }
                        }
                        if (streams == null) {
                            // waiting for attach
                            continue;
                        }
                        inPackets(streams.packetEndpoint());
                        inPackets(transport.packetEndpoint());
                        outPackets(streams.packetEndpoint());
                        outPackets(transport.packetEndpoint());
                        eventsProcessed = eventsAtStartOfLoop;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void startSocketInterceptor() {
        streams = new JdwpStreams(descriptor, socket, this);
        streams.start();
        synchronized (eventCounter) {
            eventCounter.notify();
        }
    }

    private void startTransportStream() {
        transport = new SharedVmTransport(descriptor, this);
        transport.launch();
        // FIXME - e.g. debuggee sharedvm - this
    }

    protected abstract boolean isDebuggee();

    protected abstract boolean isDebugger();

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
        oracle.start();
        try {
            startMainLoop();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
