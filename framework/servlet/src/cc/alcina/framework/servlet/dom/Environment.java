package cc.alcina.framework.servlet.dom;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.impl.DocumentContextProviderImpl;
import cc.alcina.framework.entity.impl.DocumentContextProviderImpl.DocumentFrame;

public class Environment {
	public static final transient String CONTEXT_TEST_CREDENTIALS = Environment.class
			.getName() + ".CONTEXT_TEST_CREDENTIALS";

	public final String uid;

	public final String auth;

	DocumentFrame frame;

	RemoteUi ui;

	Environment(RemoteUi ui) {
		this.ui = ui;
		if (LooseContext.is(CONTEXT_TEST_CREDENTIALS)) {
			uid = "test";
			auth = "test";
		} else {
			uid = SEUtilities.generatePrettyUuid();
			auth = SEUtilities.generatePrettyUuid();
		}
		try {
			LooseContext.push();
			DocumentContextProviderImpl.get().registerContextFrame();
			Document.get();
			frame = DocumentContextProviderImpl.get().contextInstance();
			ui.init();
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public String toString() {
		return Ax.format("env::%s", uid);
	}
}
