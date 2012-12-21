package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.appcache.client.AppCache;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

/**
 * TODO: this should be a singleton...with listeners rather than
 * 'registerUpdatingCallback' etc
 * 
 * @author nick@alcina.cc
 * 
 */
public class OfflineManager {
	private final String APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT = "Application changed on the server - please wait";

	private boolean hostPageCacheReturned = true;

	private ModalNotifier cd;

	private int updateCount;

	private OfflineManager() {
		super();
	}

	private static OfflineManager theInstance;

	public static OfflineManager get() {
		if (theInstance == null) {
			theInstance = new OfflineManager();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private Callback<Void> updatingCallback;

	private Timer appCacheResolutionTimer;

	public boolean resourceStoresCaptured() {
		return hostPageCacheReturned;
	}

	public void registerUpdatingCallback(Callback<Void> callback) {
		updatingCallback = callback;
	}

	public boolean isUpdating() {
		return cd != null;
	}

	public enum FromRequiresCurrentCachePerspsectiveReccAction {
		WAIT, CONTINUE
	}

	public FromRequiresCurrentCachePerspsectiveReccAction shouldIWait() {
		int status = AppCache.getApplicationCache().getStatus();
		AlcinaTopics.log(status);
		switch (status) {
		case AppCache.IDLE:
		case AppCache.OBSOLETE:
		case AppCache.UNCACHED:
		case AppCache.UPDATEREADY:
			return FromRequiresCurrentCachePerspsectiveReccAction.CONTINUE;
		}
		return FromRequiresCurrentCachePerspsectiveReccAction.WAIT;
	}

	public boolean checkCacheLoading(AsyncCallback completionCallback) {
		ClientLayerLocator.get().notifications()
				.log("OfflineUtils.checkCacheLoading");
		AppCache appCache = AppCache.getApplicationCache();
		AppCacheEventHandler handler = new AppCacheEventHandler(true,
				completionCallback);
		registerHandler(appCache, handler);
		handler.onBrowserEvent(null);
		if (appCache.getStatus() == AppCache.UPDATEREADY) {
			waitAndReload();
		}
		return isUpdating();
	}

	public void waitAndReload() {
		if (updatingCallback != null) {
			updatingCallback.callback(null);
		}
		cd = ClientLayerLocator.get().notifications().getModalNotifier("");
		cd.setMasking(true);
		cd.modalOn();
		AppCache appCache = AppCache.getApplicationCache();
		AppCacheEventHandler handler = new AppCacheEventHandler(false, null);
		registerHandler(appCache, handler);
		update();
	}

	protected void registerHandler(AppCache appCache,
			AppCacheEventHandler handler) {
		appCache.addEventListener(AppCache.ONCACHED, handler, true);
		appCache.addEventListener(AppCache.ONCHECKING, handler, true);
		appCache.addEventListener(AppCache.ONDOWNLOADING, handler, true);
		appCache.addEventListener(AppCache.ONERROR, handler, true);
		appCache.addEventListener(AppCache.ONNOUPDATE, handler, true);
		appCache.addEventListener(AppCache.ONPROGRESS, handler, true);
		appCache.addEventListener(AppCache.ONUPDATEREADY, handler, true);
	}

	class AppCacheEventHandler implements EventListener {
		private final boolean headless;

		private final AsyncCallback nochangeCallback;

		private boolean cancelled = false;

		public AppCacheEventHandler(boolean headless,
				AsyncCallback nochangeCallback) {
			this.headless = headless;
			this.nochangeCallback = nochangeCallback;
		}

		@Override
		public void onBrowserEvent(Event event) {
			ClientLayerLocator
					.get()
					.notifications()
					.log(CommonUtils.formatJ(
							"OfflineUtils.event - %s,%s,%s,%s", cancelled,
							headless,
							(event == null ? "null" : event.getType()),
							AppCache.getApplicationCache().getStatus()));
			if (cancelled) {
				return;
			}
			if (event != null && event.getType().equals(AppCache.ONERROR)) {
				if (updateCount != 0) {
					error("App cache error");
				}
				return;
			}
			if (event != null && event.getType().equals(AppCache.ONPROGRESS)) {
				updateCount++;
			}
			if (headless) {
				int updateStatus = AppCache.getApplicationCache().getStatus();
				if (updateStatus == AppCache.CHECKING) {
				} else if (updateStatus == AppCache.DOWNLOADING) {
					cancelled = true;
					waitAndReload();
				} else {
					if (nochangeCallback != null) {
						nochangeCallback.onSuccess(null);
					}
					return;
				}
			} else {
				update();
			}
		}
	}

	private void error(String err) {
		Window.alert(CommonUtils.formatJ("The application reload failed "
				+ "- \n Reason: %s. \n\n"
				+ " Please press 'ok' to reload the application", err));
		Window.Location.reload();
	}

	private void update() {
		int updateStatus = AppCache.getApplicationCache().getStatus();
		if (updateStatus == AppCache.CHECKING
				|| updateStatus == AppCache.DOWNLOADING) {
		} else {
			complete();
			return;
		}
		cd.setStatus(CommonUtils.formatJ("%s - %s files downloaded",
				APPLICATION_CHANGED_ON_THE_SERVER_PLEASE_WAIT, updateCount));
	}

	private void complete() {
		cd.setStatus("Complete");
		new Timer() {
			@Override
			public void run() {
				Window.Location.reload();
			}
		}.schedule(1000);
	}

	public boolean isInvalidModule(Throwable caught) {
		String s = caught.getMessage();
		return s.equals(new IncompatibleRemoteServiceException().getMessage());
	}

	public void waitUntilAppCacheResolved(final Callback callback) {
		if (shouldIWait() == FromRequiresCurrentCachePerspsectiveReccAction.CONTINUE) {
			callback.callback(null);
			return;
		}
		appCacheResolutionTimer = new Timer() {
			@Override
			public void run() {
				if (shouldIWait() == FromRequiresCurrentCachePerspsectiveReccAction.CONTINUE) {
					appCacheResolutionTimer.cancel();
					callback.callback(null);
					return;
				}
			}
		};
		appCacheResolutionTimer.scheduleRepeating(100);
	}
}
