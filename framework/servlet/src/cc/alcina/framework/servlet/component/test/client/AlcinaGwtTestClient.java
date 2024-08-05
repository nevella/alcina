package cc.alcina.framework.servlet.component.test.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.test.client.test.TestJsoLists;
import cc.alcina.framework.servlet.component.test.client.test.TestSyncMutations2;

/**
 * A scaffolding for the Alcina gwt tests
 *
 */
public class AlcinaGwtTestClient implements EntryPoint {
	@Override
	public void onModuleLoad() {
		Client.Init.init();
		Scheduler.get().scheduleDeferred(this::test);
	}

	void test() {
		if (GWT.isScript()) {
			throw new IllegalStateException("Devmode only");
		}
		new TestJsoLists().onModuleLoad();
		new TestSyncMutations2().onModuleLoad();
		ClientUtils.consoleInfo("[AlcinaGwtTestClient] Tests passed");
	}
}
