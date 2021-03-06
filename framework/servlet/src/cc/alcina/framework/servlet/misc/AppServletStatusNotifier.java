package cc.alcina.framework.servlet.misc;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.DataFolderProvider;

public class AppServletStatusNotifier {
	File dataFolder = DataFolderProvider.get().getDataFolder();

	File ready = new File(dataFolder.getPath() + "/" + "webapp.ready");

	File deploying = new File(dataFolder.getPath() + "/" + "webapp.deploying");

	File destroyed = new File(dataFolder.getPath() + "/" + "webapp.destroyed");

	File failed = new File(dataFolder.getPath() + "/" + "webapp.failed");

	public void deploying() {
		try {
			ready.delete();
			destroyed.delete();
			failed.delete();
			deploying.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void destroyed() {
		try {
			ready.delete();
			deploying.delete();
			failed.delete();
			destroyed.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void failed() {
		try {
			ready.delete();
			destroyed.delete();
			deploying.delete();
			failed.createNewFile();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void ready() {
		try {
			destroyed.delete();
			deploying.delete();
			failed.delete();
			ready.createNewFile();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
