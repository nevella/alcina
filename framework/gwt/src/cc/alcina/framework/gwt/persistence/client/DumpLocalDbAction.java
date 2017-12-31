package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsyncProvider;
import cc.alcina.framework.common.client.remote.RemoteServiceProvider;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

/**
 * Not registry annotation - so register a subclass if you want it
 * 
 * @author nick@alcina.cc
 * 
 */
public class DumpLocalDbAction implements LooseActionHandler {
	public static final String ALCINA_DUMP_LOCAL_DB = "alcina-dump-local-db";

	private ModalNotifier modalNotifier;

	@Override
	public String getName() {
		return ALCINA_DUMP_LOCAL_DB;
	}

	@Override
	public void performAction() {
		this.modalNotifier = Registry.impl(ClientNotifications.class)
				.getModalNotifier("Uploading local database dump");
		this.modalNotifier.modalOn();
		final AsyncCallbackStd postClearCallback = new AsyncCallbackStd() {
			@Override
			public void onSuccess(Object result) {
				modalNotifier.modalOff();
				Window.alert("Local database dump uploaded - database cleared");
			}
		};
		final AsyncCallback<Void> asyncCallback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}

			@Override
			public void onSuccess(Void result) {
				LocalTransformPersistence.get()
						.clearAllPersisted(postClearCallback);
			}
		};
		Callback<String> sendRpcCallback = new Callback<String>() {
			@Override
			public void apply(String value) {
				((RemoteServiceProvider<? extends CommonRemoteServiceAsync>) Registry
						.impl(CommonRemoteServiceAsyncProvider.class))
								.getServiceInstance()
								.dumpData(value, asyncCallback);
			}
		};
		LocalTransformPersistence.get().dumpDatabase(sendRpcCallback);
	}
}
