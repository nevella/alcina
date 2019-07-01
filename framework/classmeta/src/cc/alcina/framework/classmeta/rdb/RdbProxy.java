package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbProxySchemaProxyDescriptor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

@SuppressWarnings("resource")
class RdbProxy {
    Logger logger = LoggerFactory.getLogger(getClass());

    private RdbProxySchemaProxyDescriptor proxyDescriptor;

    Socket externalDebuggerSocket = null;

    Socket internalDebuggerSocket = null;

    Socket externalDebuggeeSocket = null;

    private ListeningConnector listener;

    private VirtualMachine vm;

    StreamInterceptor extDebuggerToInternalDebugger;

    StreamInterceptor extDebuggeeToExternalDebugger;

    CountDownLatch startLatch = new CountDownLatch(3);

    private Map<String, com.sun.jdi.connect.Connector.Argument> connectorArgs;

    public RdbProxy(RdbProxySchemaProxyDescriptor proxyDescriptor) {
        this.proxyDescriptor = proxyDescriptor;
    }

    public void setupLocal() {
        new Thread(Ax.format("rdb-proxy-%s", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    setupLocal0();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void connectExternalDebuggee() {
        new Thread(
                Ax.format("local-jdi-ext-debuggee-%s", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    externalDebuggeeSocket = new Socket(
                            proxyDescriptor.remoteHost,
                            proxyDescriptor.remoteJdwpPort);
                    startLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void handleEvent(Event nextEvent) throws IOException {
        Ax.out(nextEvent.getClass().getSimpleName());
        byte[] inputBytes = extDebuggerToInternalDebugger
                .markInterceptedBytes();
        externalDebuggeeSocket.getOutputStream().write(inputBytes);
    }

    private void runVmEventsQueuePump() {
        EventQueue queue = vm.eventQueue();
        while (true) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    handleEvent(it.nextEvent());
                }
                eventSet.resume();
            } catch (InterruptedException exc) {
                // Ignore
            } catch (VMDisconnectedException discExc) {
                break;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                break;
            }
        }
    }

    private void setupLocal0() {
        setupLocalTunnel();
        setupLocalListeningConnection();
        try {
            startLatch.await();
            logger.info("All sockets connected - {}", proxyDescriptor.name);
            internalDebuggerSocket = new Socket("127.0.0.1",
                    proxyDescriptor.localJdwpPort);
            extDebuggerToInternalDebugger = new StreamInterceptor(
                    Ax.format("local-debug-%s-extDebuggerToInternalDebuggee",
                            proxyDescriptor.name),
                    externalDebuggerSocket.getInputStream(),
                    internalDebuggerSocket.getOutputStream());
            extDebuggeeToExternalDebugger = new StreamInterceptor(
                    Ax.format("local-debug-%s-extDebuggeeToExternalDebugger",
                            proxyDescriptor.name),
                    externalDebuggeeSocket.getInputStream(),
                    externalDebuggerSocket.getOutputStream());
            extDebuggerToInternalDebugger.start();
            extDebuggeeToExternalDebugger.start();
            // runVmEventsQueuePump();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void setupLocalListeningConnection() {
        try {
            for (Connector connector : Bootstrap.virtualMachineManager()
                    .allConnectors()) {
                if (connector.name().equals("com.sun.jdi.SocketListen")) {
                    this.listener = (ListeningConnector) connector;
                }
            }
            connectorArgs = this.listener.defaultArguments();
            connectorArgs.get("port")
                    .setValue(String.valueOf(proxyDescriptor.localJdwpPort));
            String retAddress = listener.startListening(connectorArgs);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        new Thread(Ax.format("local-jdi-acceptor-%s", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    setupLocalListeningConnection0();
                    startLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void setupLocalListeningConnection0() throws Exception {
        vm = listener.accept(connectorArgs);
        listener.stopListening(connectorArgs);
    }

    private void setupLocalTunnel() {
        new Thread(Ax.format("local-debug-acceptor-%s", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    listenOnLocalTunnelPort();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    void listenOnLocalTunnelPort() throws IOException, Exception {
        ServerSocket serverSocket = new ServerSocket(proxyDescriptor.localPort);
        while (true) {
            try {
                Socket newSocket = serverSocket.accept();
                if (externalDebuggerSocket != null) {
                    throw new IOException(
                            "Only one simultaneous connection allowed");
                }
                externalDebuggerSocket = newSocket;
                startLatch.countDown();
                connectExternalDebuggee();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class StreamInterceptor {
        int mark = 0;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private String name;

        private InputStream fromStream;

        private OutputStream toStream;

        StreamInterceptor(String name, InputStream inputStream,
                OutputStream outputStream) {
            this.name = name;
            this.fromStream = inputStream;
            this.toStream = outputStream;
        }

        public byte[] markInterceptedBytes() {
            synchronized (buffer) {
                byte[] bytes = new byte[buffer.size() - mark];
                System.arraycopy(buffer, mark, bytes, 0, bytes.length);
                mark += bytes.length;
                return bytes;
            }
        }

        public void start() {
            new Thread(name) {
                @Override
                public void run() {
                    try {
                        while (true) {
                            int b = fromStream.read();
                            if (b == -1) {
                                break;
                            }
                            synchronized (buffer) {
                                buffer.write(b);
                            }
                            toStream.write(b);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}