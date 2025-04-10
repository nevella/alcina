package cc.alcina.framework.servlet.component.test.client;

import java.util.stream.Collectors;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.DomContext;
import cc.alcina.framework.gwt.client.util.DomContextGwt;
import cc.alcina.framework.gwt.client.util.TimerGwt;

/**
 * A scaffolding for the Alcina gwt tests
 *
 */
public class AlcinaGwtTestClient implements EntryPoint {
	@Override
	public void onModuleLoad() {
		DomDocument.useLocations2 = true;
		Client.Init.init();
		Registry.register().singleton(Timer.Provider.class,
				new TimerGwt.Provider());
		Registry.register().singleton(DomContext.class, new DomContextGwt());
		Scheduler.get().scheduleDeferred(this::test);
	}

	void test() {
		if (GWT.isScript()) {
			throw new IllegalStateException("Devmode only");
		}
		// new TestJsoLists().run();
		// new TestSyncMutations2().run();
		// new TestSyncMutations2a().run();
		// new TestChubbyTree().run();
		new TestLocationMutation().run();
		ClientUtils.consoleInfo("[AlcinaGwtTestClient] Tests passed");
	}

	static class Utils {
		static void clearRootPanel() {
			RootPanel.get().clear();
			RootPanel.get().getElement().streamChildren().filter(
					n -> !DomNode.from(n).tagIsOneOf("iframe", "script"))
					.collect(Collectors.toList())
					.forEach(Node::removeFromParent);
		}
	}
}
