package cc.alcina.framework.servlet.servlet.wd;

import com.google.gwt.dom.client.DomEventData;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface WebdriverService {
	public static WebdriverService get() {
		return Registry.impl(WebdriverService.class);
	}

	public interface WebdriverSession {
		void init();

		void navigateTo(String url);

		void performEvent(DomEventData event);

		String getDocumentAttribute(String attrServerMessageProcessed);
	}

	WebdriverSession createSession();
}
