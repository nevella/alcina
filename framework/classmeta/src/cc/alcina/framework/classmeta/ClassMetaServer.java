package cc.alcina.framework.classmeta;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class ClassMetaServer {
    public static void main(String[] args) {
        try {
            new ClassMetaServer().start();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void start() throws Exception {
        int port = 10005;
        Server server = new Server(port);
        HandlerCollection handlers = new HandlerCollection(true);
        ClassMetaHandler metaHandler = new ClassMetaHandler();
        {
            ContextHandler ctx = new ContextHandler(handlers, "/meta");
            ctx.setHandler(metaHandler);
            handlers.addHandler(ctx);
        }
        {
            ContextHandler ctx = new ContextHandler(handlers, "/persistence");
            ctx.setHandler(new ClassPersistenceScanHandler(metaHandler));
            handlers.addHandler(ctx);
        }
        {
            ContextHandler ctx = new ContextHandler(handlers, "/ant");
            ctx.setHandler(new AntHandler());
            handlers.addHandler(ctx);
        }
        server.setHandler(handlers);
        server.start();
        server.dumpStdErr();
        server.join();
    }
}
