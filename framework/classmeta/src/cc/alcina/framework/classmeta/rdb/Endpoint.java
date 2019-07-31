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
import cc.alcina.framework.common.client.util.TimeConstants;

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

    Oracle oracle;

    Logger logger = LoggerFactory.getLogger(getClass());

    Socket socket = null;

    AtomicInteger receivedPacketCounter = new AtomicInteger(0);

    int inPacketCounter = 0;

    long lastPacketFromOtherEndpointMs = 0;

    int predictiveReplyPacketCounter = 0;

    private ServerSocket serverSocket;

    boolean closed = false;

    public Endpoint(RdbEndpointDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void close() {
        if (closed) {
            return;
        }
        logger.warn("Closing :: {}", descriptor);
        closed = true;
        streams.setClosed(true);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        transport.close();
        synchronized (receivedPacketCounter) {
            receivedPacketCounter.notify();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        RdbProxies.get().replaceEndpoint(this);
    }

    // bit of an abuse of the counter - but it's logically the "should I send"
    // monitor
    public void nudge() {
        synchronized (receivedPacketCounter) {
            receivedPacketCounter.notify();
        }
    }

    public PacketEndpoint otherPacketEndpoint(PacketEndpoint packetEndpoint) {
        return packetEndpoint.host == streams ? transport.packetEndpoint()
                : streams.packetEndpoint();
    }

    public void packetsReceived(Packet packet) {
        if (!packet.fromDebugger) {
            packet.fromDebugger = this.isDebugger() && packet.source != null
                    && packet.source.host == streams;
        }
        if (packet.isPredictive) {
            oracle.receivedPredictivePacket();
        } else {
            synchronized (receivedPacketCounter) {
                receivedPacketCounter.incrementAndGet();
                receivedPacketCounter.notify();
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
            inPacketCounter++;
            if (packet.source == transport.packetEndpoint()) {
                lastPacketFromOtherEndpointMs = System.currentTimeMillis();
            } else {
                if (lastPacketFromOtherEndpointMs != 0
                        && System.currentTimeMillis()
                                - lastPacketFromOtherEndpointMs > 5
                                        * TimeConstants.ONE_MINUTE_MS) {
                    close();
                    return;
                }
            }
            oracle.preparePacket(packet);
            Optional<Packet> predictiveResponse = otherPacketEndpoint
                    .getPredictiveResponse(packet);
            if (predictiveResponse.isPresent()) {
                // if (packet.messageName.equals("Status")) {
                // int debug = 3;
                // }
                packetEndpoint.addReplyPacket(predictiveResponse.get());
                logger.debug("Predictive packet << {}\t{}", packetEndpoint,
                        packet);
                predictiveReplyPacketCounter++;
                break;
            }
            if (packet.fromDebugger && isDebugger()
                    && packet.source == streams.packetEndpoint()) {
                oracle.beforePacketMiss(packet);
            }
            oracle.analysePacket(packet);
            oracle.handlePacket(packetEndpoint, packet);
            /*
             * logic: if the predicitive packets can't answer all jdwp requests,
             * they may be out of date...
             */
            otherPacketEndpoint.onPredictivePacketMiss();
            oracle.onPredictivePacketMiss();
            if (packetEndpoint.host instanceof Transport) {
                logger.debug("Received packet :: {}\t{}", packetEndpoint,
                        packet);
            } else {
                logger.info("Received packet :: {}\t{}", packetEndpoint,
                        packet);
                // packet.dump();
                logger.info("Packets: {}/{}", predictiveReplyPacketCounter,
                        inPacketCounter);
            }
            otherPacketEndpoint.addOutPacket(packet);
            otherPacketEndpoint.host.addPredictivePackets(
                    packetEndpoint.flushPredictivePackets());
            if (packet.meta.mustSend) {
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
        serverSocket = new ServerSocket(descriptor.jdwpPort);
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
                if (!closed) {
                    e.printStackTrace();
                }
                break;
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
                        synchronized (receivedPacketCounter) {
                            // the main thing is to not miss wakeups - so if an
                            // incoming packet is received on another thread
                            // during this loop (and the counter is incremented
                            // outside this sync block) - loop will rerun
                            //
                            // who knows - there may be a .concurrent class that
                            // does this too
                            eventsAtStartOfLoop = receivedPacketCounter.get();
                            if (eventsAtStartOfLoop == eventsProcessed) {
                                receivedPacketCounter.wait();
                            }
                        }
                        if (closed) {
                            return;
                        }
                        if (streams == null) {
                            if (descriptor.jdwpAttach) {
                                doAttachJdwp();
                            } else {
                                // waiting for attach
                                continue;
                            }
                        }
                        if (oracle == null) {
                            oracle = new Oracle(Endpoint.this);
                            oracle.start();
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
        synchronized (receivedPacketCounter) {
            receivedPacketCounter.notify();
        }
    }

    private void startTransportStream() {
        switch (descriptor.transportType) {
        case shared_vm:
            transport = new SharedVmTransport(descriptor, this);
            break;
        case http_initiator:
            transport = new HttpInitiatorTransport(descriptor, this);
            break;
        case http_acceptor:
            transport = new HttpAcceptorTransport(descriptor, this);
            break;
        default:
            throw new UnsupportedOperationException();
        }
        transport.launch();
    }

    protected abstract boolean isDebuggee();

    protected abstract boolean isDebugger();

    protected void startJdwpStream() {
        if (descriptor.jdwpAttach) {
            // doAttachJdwp();
            // wait for first received packet
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
