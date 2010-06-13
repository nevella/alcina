package cc.alcina.framework.gwt.gears.client;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.gears.client.Factory;
import com.google.gwt.gears.client.localserver.LocalServer;
import com.google.gwt.gears.client.localserver.ResourceStore;
import com.google.gwt.gears.client.localserver.ResourceStoreUrlCaptureHandler;
import com.google.gwt.user.client.Window;

public class LocalServerUtils {
	private static final int MAX_STORE_NAME_LENGTH = 64;

	public static void cacheHostPage() {
		String storeName = "host_page_" + GWT.getModuleName() + "_offline";
		if (storeName.length() > MAX_STORE_NAME_LENGTH) {
			storeName = storeName.substring(storeName.length()
					- MAX_STORE_NAME_LENGTH);
		}
		assert storeName.length() <= MAX_STORE_NAME_LENGTH;
		LocalServer server = Factory.getInstance().createLocalServer();
		ResourceStore store = server.createStore(storeName);
		String path = Window.Location.getPath();
		path = path.substring(path.lastIndexOf("/") + 1);
		store.capture(new ResourceStoreUrlCaptureHandler() {
			public void onCapture(ResourceStoreUrlCaptureEvent event) {
				// hopefully ok
			}
		}, path);
		// note - we can't capture a gwt.codesvr string here - throw an error on
		// remoteserviceimpl.hello()
		// to simulate offline
	}
}
