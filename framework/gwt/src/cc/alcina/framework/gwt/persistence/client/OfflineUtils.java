package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.appcache.client.AppCache;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

public class OfflineUtils {
	private static final String APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT = "Application changed on the server - please wait";

	private static boolean hostPageCacheReturned = true;

	private static NonCancellableRemoteDialog cd;

	public static boolean resourceStoresCaptured() {
		return hostPageCacheReturned;
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
		AppCache appCache = AppCache.getApplicationCache();
		AppCacheEventHandler handler = new AppCacheEventHandler();
		appCache.addEventListener(AppCache.ONCACHED, handler, true);
		appCache.addEventListener(AppCache.ONCHECKING, handler, true);
		appCache.addEventListener(AppCache.ONDOWNLOADING, handler, true);
		appCache.addEventListener(AppCache.ONERROR, handler, true);
		appCache.addEventListener(AppCache.ONNOUPDATE, handler, true);
		appCache.addEventListener(AppCache.ONPROGRESS, handler, true);
		appCache.addEventListener(AppCache.ONUPDATEREADY, handler, true);
		update(0);
	}

	static class AppCacheEventHandler implements EventListener {
		@Override
		public void onBrowserEvent(Event event) {
			if (event.getType().equals(AppCache.ONERROR)) {
				error("App cache error");
				return;
			}
			update(0);
		}
	}

	private static void error(String err) {
		Window.alert(CommonUtils.formatJ("The application reload failed "
				+ "- \n Reason: %s. \n\n"
				+ " Please press 'ok' to reload the application", err));
		Window.Location.reload();
	}

	private static void update(double d) {
		int updateStatus = AppCache.getApplicationCache().getStatus();
		if (updateStatus == AppCache.CHECKING
				|| updateStatus == AppCache.DOWNLOADING) {
		} else {
			complete();
			return;
		}
		cd.setStatus(CommonUtils.formatJ("%s - %s% complete",
				APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT,
				Math.ceil(d * 100)));
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
