package cc.alcina.framework.classmeta.rdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

@SuppressWarnings("resource")
class RdbProxy {
    Logger logger = LoggerFactory.getLogger(getClass());

    private RdbProxySchemaProxyDescriptor proxyDescriptor;

    Socket externalDebuggerSocket = null;

    Socket internalDebuggerSocket = null;

    Socket externalDebuggeeSocket = null;

    private ListeningConnector listener;

    private VirtualMachine vm;

    StreamInterceptor extDebuggerToExtDebugee;

    StreamInterceptor extDebuggeeToExternalDebugger;

    CountDownLatch startLatch = new CountDownLatch(2);

    private Map<String, com.sun.jdi.connect.Connector.Argument> connectorArgs;

    private State state = new State();

    private StreamListener streamListener = new StreamListener() {
        @Override
        public synchronized void byteReceived(StreamInterceptor interceptor,
                int b) {
            if (state.lastSender == null) {
            } else {
                if (state.lastSender != interceptor) {
                    byte[] bytes = state.lastSender.lastSent;
                    String s = new String(bytes, StandardCharsets.UTF_8);
                    boolean fromExtDebugger = state.lastSender == extDebuggerToExtDebugee;
                    String dir = fromExtDebugger ? ">>>" : "<<<";
                    s = "...";
                    Ax.out("%s : %s %s\n", state.lastSender.name, dir, s);
                    SEUtilities.dumpBytes(bytes, 11);
                    if (!fromExtDebugger) {
                        try {
                            internalDebuggerSocket.getOutputStream()
                                    .write(bytes);
                        } catch (IOException e) {
                            throw new WrappedRuntimeException(e);
                        }
                        Ax.out("(Echoed to local debugger)");
                    }
                }
            }
            state.lastSender = interceptor;
        }
    };

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

    public void startLocalReader() {
        new Thread(Ax.format("rdb-local-reader-%s", proxyDescriptor.name)) {
            private AtEndOfEventSeriesTimer seriesTimer = new AtEndOfEventSeriesTimer(
                    5, new Runnable() {
                        @Override
                        public void run() {
                            synchronized (buffer) {
                                byte[] bytes = buffer.toByteArray();
                                Ax.out("Read %s bytes from internal debugger output",
                                        bytes.length);
                                buffer.reset();
                            }
                        }
                    }).maxDelayFromFirstAction(5);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void run() {
                try {
                    while (true) {
                        int b = internalDebuggerSocket.getInputStream().read();
                        if (b == -1) {
                            break;
                        }
                        synchronized (buffer) {
                            buffer.write(b);
                        }
                        seriesTimer.triggerEventOccurred();
                    }
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
                    logger.info("Ext debuggee connected to socket - {}",
                            proxyDescriptor.name);
                    startLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void handleEvent(Event nextEvent) throws IOException {
        Ax.out(nextEvent.getClass().getSimpleName());
        // byte[] inputBytes = extDebuggerToExtDebugee.lastSent;
        // externalDebuggeeSocket.getOutputStream().write(inputBytes);
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
            // hack
            extDebuggerToExtDebugee = new StreamInterceptor(
                    Ax.format("local-debug-%s-extDebuggerToInternalDebuggee",
                            proxyDescriptor.name),
                    streamListener, externalDebuggerSocket.getInputStream(),
                    externalDebuggeeSocket.getOutputStream());
            extDebuggeeToExternalDebugger = new StreamInterceptor(
                    Ax.format("local-debug-%s-extDebuggeeToExternalDebugger",
                            proxyDescriptor.name),
                    streamListener, externalDebuggeeSocket.getInputStream(),
                    externalDebuggerSocket.getOutputStream());
            extDebuggerToExtDebugee.start();
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
            internalDebuggerSocket = new Socket("127.0.0.1",
                    proxyDescriptor.localJdwpPort);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        new Thread(Ax.format("local-jdi-acceptor-%s", proxyDescriptor.name)) {
            @Override
            public void run() {
                try {
                    setupLocalListeningConnection0();
                    logger.info("Local jdi started listening - {}",
                            proxyDescriptor.name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        startLocalReader();
    }

    private void setupLocalListeningConnection0() throws Exception {
        vm = listener.accept(connectorArgs);
        listener.stopListening(connectorArgs);
        runVmEventsQueuePump();
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
                logger.info("Ext debugger connected to stream interceptor - {}",
                        proxyDescriptor.name);
                startLatch.countDown();
                connectExternalDebuggee();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class State {
        StreamInterceptor lastSender = null;
    }

    static class StreamInterceptor {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] lastSent;

        private String name;

        private InputStream fromStream;

        private OutputStream toStream;

        private StreamListener streamListener;

        private AtEndOfEventSeriesTimer seriesTimer = new AtEndOfEventSeriesTimer(
                5, new Runnable() {
                    @Override
                    public void run() {
                        writeToToStream();
                    }
                }).maxDelayFromFirstAction(5);

        StreamInterceptor(String name, StreamListener streamListener,
                InputStream inputStream, OutputStream outputStream) {
            this.name = name;
            this.streamListener = streamListener;
            this.fromStream = inputStream;
            this.toStream = outputStream;
            Ax.out("[%s] : %s >> %s", name, fromStream, toStream);
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
                            streamListener.byteReceived(StreamInterceptor.this,
                                    b);
                            synchronized (buffer) {
                                buffer.write(b);
                            }
                            seriesTimer.triggerEventOccurred();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        protected void writeToToStream() {
            try {
                synchronized (buffer) {
                    byte[] bytes = buffer.toByteArray();
                    buffer.reset();
                    lastSent = bytes;
                    toStream.write(bytes);
                    toStream.flush();
                }
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }
    }

    interface StreamListener {
        void byteReceived(StreamInterceptor interceptor, int b);
    }
}