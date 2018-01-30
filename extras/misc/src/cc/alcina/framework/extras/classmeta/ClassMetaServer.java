package cc.alcina.framework.extras.classmeta;

import org.eclipse.jetty.server.Server;

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
		Server server = new Server(10005);
        server.start();
        server.dumpStdErr();
        server.join();
	}
}
