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

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbProxySchemaProxyDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;

@SuppressWarnings("resource")
class RdbProxy {
    Logger logger = LoggerFactory.getLogger(getClass());

    private RdbProxySchemaProxyDescriptor proxyDescriptor;

    Socket externalDebuggerSocket = null;

    Socket externalDebuggeeSocket = null;

    StreamInterceptor extDebuggerToExtDebugee;

    StreamInterceptor extDebuggeeToExternalDebugger;

    CountDownLatch startLatch = new CountDownLatch(1);

    private State state = new State();

    boolean record = false;

    RdbPackets packets = new RdbPackets();

    CountDownLatch waitForInternalDebugger;
    
    VirtualMachineImplExt vm = new VirtualMachineImplExt();

    private StreamListener ioStreamListener = new StreamListener() {
        int receivedCount = 0;

        @Override
        public synchronized void byteReceived(StreamInterceptor interceptor,
                int b) {
            if (state.lastSender == null) {
            } else {
                if (state.lastSender != interceptor) {
                    byte[] bytes = state.lastSender.lastSent;
                    String s = new String(bytes, StandardCharsets.UTF_8);
                    boolean fromExtDebugger = state.lastSender == extDebuggerToExtDebugee;
                    RdbPacket packet = new RdbPacket();
                    packet.fromDebugger = fromExtDebugger;
                    packet.bytes = bytes;
                    packet.fromName = state.lastSender.name;
                    packet.dump();
                    packets.packets.add(packet);
                    if (record) {
                        appendPacket(packet);
                    }
                    if (++receivedCount <= 8) {
                        Ax.out("Ignoring early packet: %s", receivedCount);
                    } else {
                        parsePacket(packet);
                    }
                }
            }
            state.lastSender = interceptor;
        }

    };
    private void parsePacket(RdbPacket packet) {
    }

    public RdbProxy(RdbProxySchemaProxyDescriptor proxyDescriptor) {
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

    private void appendPacket(RdbPacket packet) {
        String fn = Ax.format(
                "/private/var/local/git/alcina/framework/classmeta/src/cc/alcina/framework/classmeta/rdb/packets-%s.json",
                proxyDescriptor.name);
        ResourceUtilities.write(
                JacksonUtils.serializeForLoggingWithDefaultsNoTypes(packets),
                fn);
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

    private void handleEvent(Event nextEvent) throws IOException {
        Ax.out(nextEvent.getClass().getSimpleName());
        // byte[] inputBytes = extDebuggerToExtDebugee.lastSent;
        // externalDebuggeeSocket.getOutputStream().write(inputBytes);
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
            extDebuggerToExtDebugee = new StreamInterceptor(
                    Ax.format("%s::extDebuggerToExtDebuggee",
                            proxyDescriptor.name),
                    ioStreamListener, externalDebuggerSocket.getInputStream(),
                    externalDebuggeeSocket.getOutputStream());
            extDebuggeeToExternalDebugger = new StreamInterceptor(
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
        StreamInterceptor lastSender = null;
    }

    interface StreamListener {
        void byteReceived(StreamInterceptor interceptor, int b);
    }
}