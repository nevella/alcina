package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;

public abstract class LocalTransformPersistenceGwt extends LocalTransformPersistence implements  ClosingHandler {
	public LocalTransformPersistenceGwt() {
		Window.addWindowClosingHandler(this);
	}
	public void onWindowClosing(ClosingEvent event) {
		setClosing(true);
	}
}
