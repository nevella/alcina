package cc.alcina.extras.dev.component.remote.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;

import cc.alcina.extras.dev.component.remote.client.common.logic.RemoteComponentInit;
import cc.alcina.framework.gwt.client.Client;

/**
 * Thin gwt app which manipulates dom, posts events via send/receive on the the
 * rc-rpc protocol channel
 * 
 * @author nick@alcina.cc
 *
 */
public class RemoteComponentClient implements EntryPoint {
	@Override
	public void onModuleLoad() {
		Client.Init.init();
		Scheduler.get().scheduleDeferred(() -> init0());
	}

	private void init0() {
		new RemoteComponentInit().init();
	}
}
