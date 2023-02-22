package cc.alcina.extras.dev.console.remote.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleInit;
import cc.alcina.framework.gwt.client.Client;

//@formatter:off
/*
 * Nits:
 *
 * up arrow and enter key  should populate command box
 * show status up top
 * autoshow on start?
 * navigation to eclipse file/line
 *
 * @author nick@alcina.cc
 *
 */
// @formatter:on
public class RemoteConsoleClient implements EntryPoint {
	@Override
	public void onModuleLoad() {
		Client.Init.init();
		Scheduler.get().scheduleDeferred(() -> init0());
	}

	private void init0() {
		new RemoteConsoleInit().init();
		RemoteConsoleLayout.get().init();
	}
}
