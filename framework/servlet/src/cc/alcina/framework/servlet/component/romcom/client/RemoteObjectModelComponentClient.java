package cc.alcina.framework.servlet.component.romcom.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentUi;

/**
 * Thin gwt app which manipulates dom, posts events via send/receive on the the
 * rc-rpc protocol channel
 *
 * 
 *
 */
public class RemoteObjectModelComponentClient implements EntryPoint {
	public static native void consoleError(String s) /*-{
    try {
      $wnd.console.error(s);
    } catch (e) {

    }
	}-*/;

	private void init0() {
		GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void onUncaughtException(Throwable e) {
				e.printStackTrace();
				consoleError(CommonUtils.toSimpleExceptionMessage(e));
			}
		});
		new RemoteComponentUi().init();
	}

	@Override
	public void onModuleLoad() {
		Client.Init.init();
		Scheduler.get().scheduleDeferred(() -> init0());
	}
}
