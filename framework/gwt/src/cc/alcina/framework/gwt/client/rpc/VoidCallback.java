package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.gwt.client.util.AsyncCallback;

public class VoidCallback implements AsyncCallback {
	public void onFailure(Throwable caught) {
		// fail silently
	}

	public void onSuccess(Object result) {
	}
}