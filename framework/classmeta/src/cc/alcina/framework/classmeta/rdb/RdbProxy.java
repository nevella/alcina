package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.event.Event;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

@SuppressWarnings("resource")
class RdbProxy {
    Logger logger = LoggerFactory.getLogger(getClass());

    private RdbEndpointDescriptor proxyDescriptor;

    Socket externalDebuggerSocket = null;

    Socket externalDebuggeeSocket = null;

    JdwpStreams extDebuggerToExtDebugee;

    JdwpStreams extDebuggeeToExternalDebugger;

    CountDownLatch startLatch = new CountDownLatch(1);

    private State state = new State();

    boolean record = true;

    Packets packets = new Packets();

    CountDownLatch waitForInternalDebugger;

    VirtualMachineImplExt vm = new VirtualMachineImplExt();

    private StreamListener ioStreamListener = new StreamListener() {
        @Override
        public void packetReceived(JdwpStreams interceptor,
                Packet packet) {
            packet.fromDebugger = interceptor == extDebuggerToExtDebugee;
            parsePacket(packet);
            packets.add(packet);
            packet.dump();
            if (record) {
                appendPacket(packet);
                interceptor.write(packet);
            }
        }
    };

    Accessor jwdpAccessor = new Accessor();

    private void parsePacket(Packet packet) {
        jwdpAccessor.parse(packet);
    }

    public RdbProxy(RdbEndpointDescriptor proxyDescriptor) {
        this.proxyDescriptor = proxyDescriptor;
    }

    public void start() {
        new Thread(Ax.format("%s::rdb-proxy", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    openInterceptorSockets();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private AtEndOfEventSeriesTimer<ClientLogRecord> appendTimer = new AtEndOfEventSeriesTimer<>(
            200, new Runnable() {
                @Override
                public void run() {
                    String fn = Ax.format(
                            "/private/var/local/git/alcina/framework/classmeta/src/cc/alcina/framework/classmeta/rdb/packets-%s.json",
                            proxyDescriptor.name);
                    ResourceUtilities.write(JacksonUtils
                            .serializeForLoggingWithDefaultsNoTypes(packets),
                            fn);
                }
            }).maxDelayFromFirstAction(200);

    private void appendPacket(Packet packet) {
        appendTimer.triggerEventOccurred();
    }

    private void connectExternalDebuggee() {
        try {
            externalDebuggeeSocket = new Socket(proxyDescriptor.remoteHost,
                    proxyDescriptor.remoteJdwpPort);
            logger.info("Ext debuggee connected to socket - {}",
                    proxyDescriptor.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForExternalDebuggerAttach() {
        new Thread(Ax.format("%s::external-debugger-attach",
                proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    listenForExternalDebuggerAttach0();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void listenForExternalDebuggerAttach0()
            throws IOException, Exception {
        ServerSocket serverSocket = new ServerSocket(
                proxyDescriptor.externalDebuggerAttachToPort);
        while (true) {
            try {
                Socket newSocket = serverSocket.accept();
                if (externalDebuggerSocket != null) {
                    throw new IOException(
                            "Only one simultaneous connection allowed");
                }
                externalDebuggerSocket = newSocket;
                logger.info("Ext debugger connected to stream interceptor - {}",
                        proxyDescriptor.name);
                connectExternalDebuggee();
                startLatch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openInterceptorSockets() {
        listenForExternalDebuggerAttach();
        try {
            startLatch.await();
            logger.info("All sockets connected - {}", proxyDescriptor.name);
            extDebuggerToExtDebugee = new JdwpStreams(
                    Ax.format("%s::extDebuggerToExtDebuggee",
                            proxyDescriptor.name),
                    ioStreamListener, externalDebuggerSocket.getInputStream(),
                    externalDebuggeeSocket.getOutputStream());
            extDebuggeeToExternalDebugger = new JdwpStreams(
                    Ax.format("%s::extDebuggeeToExtDebugger",
                            proxyDescriptor.name),
                    ioStreamListener, externalDebuggeeSocket.getInputStream(),
                    externalDebuggerSocket.getOutputStream());
            extDebuggerToExtDebugee.start();
            extDebuggeeToExternalDebugger.start();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    class State {
        JdwpStreams lastSender = null;
    }

    interface StreamListener {
        void packetReceived(JdwpStreams streams,
                Packet received);
    }
}