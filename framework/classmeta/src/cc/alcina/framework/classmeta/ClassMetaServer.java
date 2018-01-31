package cc.alcina.framework.classmeta;

import org.eclipse.jetty.server.Server;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

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
		server.setHandler(new ClassMetaHandler());
		server.start();
		server.dumpStdErr();
		server.join();
	}
}
