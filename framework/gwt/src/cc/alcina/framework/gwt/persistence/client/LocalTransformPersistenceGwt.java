package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

public abstract class LocalTransformPersistenceGwt extends LocalTransformPersistence implements  ClosingHandler {
	public LocalTransformPersistenceGwt() {
		Window.addWindowClosingHandler(this);
	}
	public void onWindowClosing(ClosingEvent event) {
		setClosing(true);
	}
}
