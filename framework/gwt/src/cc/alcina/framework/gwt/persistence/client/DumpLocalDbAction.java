package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
	public void performAction() {
		this.modalNotifier = ClientLayerLocator.get().notifications()
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
			public void onSuccess(Void result) {
				LocalTransformPersistence.get().clearAllPersisted(postClearCallback);
			}

			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}
		};
		Callback<String> sendRpcCallback = new Callback<String>() {
			@Override
			public void apply(String value) {
				ClientLayerLocator.get().getCommonRemoteServiceAsyncProvider()
						.getServiceInstance().dumpData(value, asyncCallback);
			}
		};
		
		LocalTransformPersistence.get().dumpDatabase(sendRpcCallback);
	}

	@Override
	public String getName() {
		return ALCINA_DUMP_LOCAL_DB;
	}
}
