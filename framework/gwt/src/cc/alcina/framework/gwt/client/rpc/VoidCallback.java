package cc.alcina.framework.gwt.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class VoidCallback implements AsyncCallback {
	public void onFailure(Throwable caught) {
		// fail silently
	}

	public void onSuccess(Object result) {
	}
}