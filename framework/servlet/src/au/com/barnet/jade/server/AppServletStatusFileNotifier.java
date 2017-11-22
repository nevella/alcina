package au.com.barnet.jade.server;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.servlet.servlet.DataFolderProvider;

public class AppServletStatusFileNotifier {
	File dataFolder = DataFolderProvider.get().getDataFolder();

	File ready = new File(dataFolder.getPath() + "/" + "webapp.ready");

	File deploying = new File(dataFolder.getPath() + "/" + "webapp.deploying");

	File destroyed = new File(dataFolder.getPath() + "/" + "webapp.destroyed");

	public void ready() {
		try {
			destroyed.delete();
			deploying.delete();
			ready.createNewFile();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void destroyed() {
		try {
			ready.delete();
			deploying.delete();
			destroyed.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deploying() {
		try {
			ready.delete();
			destroyed.delete();
			deploying.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
