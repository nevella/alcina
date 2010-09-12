package cc.alcina.framework.gwt.gears.client;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.OkDialogBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.gears.client.Factory;
import com.google.gwt.gears.client.GearsException;
import com.google.gwt.gears.client.localserver.LocalServer;
import com.google.gwt.gears.client.localserver.ManagedResourceStore;
import com.google.gwt.gears.client.localserver.ManagedResourceStoreCompleteHandler;
import com.google.gwt.gears.client.localserver.ManagedResourceStoreErrorHandler;
import com.google.gwt.gears.client.localserver.ManagedResourceStoreProgressHandler;
import com.google.gwt.gears.client.localserver.ResourceStore;
import com.google.gwt.gears.client.localserver.ResourceStoreUrlCaptureHandler;
import com.google.gwt.gears.offline.client.Offline;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

public class LocalServerUtils {
	private static final String APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT = "Application changed on the server - please wait";

	private static final int MAX_STORE_NAME_LENGTH = 64;

	private static boolean hostPageCacheReturned = true;

	private static NonCancellableRemoteDialog cd;

	public static boolean resourceStoresCaptured() {
		return hostPageCacheReturned;
	}

	public static void cacheHostPage() {
		hostPageCacheReturned = false;
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
		ClientLayerLocator.get().notifications().log(
				"capturing host page:" + path);
		store.capture(new ResourceStoreUrlCaptureHandler() {
			public void onCapture(ResourceStoreUrlCaptureEvent event) {
				// hopefully ok
				hostPageCacheReturned = true;
			}
		}, path);
		// note - we can't capture a gwt.codesvr string here - throw an error on
		// remoteserviceimpl.hello()
		// to simulate offline
	}

	public static void waitAndReload() {
		cd = new NonCancellableRemoteDialog("") {
			@Override
			protected boolean initialAnimationEnabled() {
				return false;
			}
		};
		cd.getGlass().setOpacity(0);
		cd.show();
		try {
			ManagedResourceStoreHandler handler = new ManagedResourceStoreHandler();
			ManagedResourceStore store = Offline.getManagedResourceStore();
			store.setOnCompleteHandler(handler);
			store.setOnErrorHandler(handler);
			store.setOnProgressHandler(handler);
			update(0);
		} catch (GearsException e) {
			LocalServerUtils.error(e.getMessage());
		}
	}

	static class ManagedResourceStoreHandler implements
			ManagedResourceStoreCompleteHandler,
			ManagedResourceStoreErrorHandler,
			ManagedResourceStoreProgressHandler {
		public void onComplete(ManagedResourceStoreCompleteEvent event) {
			complete();
		}

		public void onError(ManagedResourceStoreErrorEvent error) {
			LocalServerUtils.error(error.getMessage());
		}

		public void onProgress(ManagedResourceStoreProgressEvent event) {
			LocalServerUtils.update(event.getFilesTotal() == 0 ? 1 : event
					.getFilesComplete()
					/ event.getFilesTotal());
		}
	}

	private static void error(String err) {
		Window.alert(CommonUtils.formatJ("The application reload failed "
				+ "- \n Reason: %s. \n\n"
				+ " Please press 'ok' to reload the application", err));
		Window.Location.reload();
	}

	private static void update(double d) {
		try {
			int updateStatus = Offline.getManagedResourceStore()
					.getUpdateStatus();
			if (updateStatus == ManagedResourceStore.UPDATE_FAILED) {
				error("Unknown");
				return;
			}
			if (updateStatus == ManagedResourceStore.UPDATE_OK) {
				complete();
				return;
			}
		} catch (GearsException e) {
			error(e.getMessage());
			return;
		}
		cd.setStatus(CommonUtils.formatJ("%s - %s% complete",
				APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT, Math
						.ceil(d * 100)));
	}

	private static void complete() {
		cd.setStatus("Complete");
		new Timer() {
			@Override
			public void run() {
				Window.Location.reload();
			}
		}.schedule(1500);
	}

	public static boolean isInvalidModule(Throwable caught) {
		String s = caught.getMessage();
		return s.equals(new IncompatibleRemoteServiceException().getMessage());
	}
}
