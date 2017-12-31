package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsyncProvider;
import cc.alcina.framework.common.client.remote.RemoteServiceProvider;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

/**
 * Not registry annotation - so register a subclass if you want it
 * 
 * @author nick@alcina.cc
 * 
 */
public class RestoreLocalDbAction implements LooseActionHandler {
	public static final String ALCINA_RESTORE_LOCAL_DB = "alcina-restore-local-db";

	public static final String ALCINA_RESTORE_LOCAL_DB_KEY = "key";

	private ModalNotifier modalNotifier;

	@Override
	public String getName() {
		return ALCINA_RESTORE_LOCAL_DB;
	}

	@Override
	public void performAction() {
		this.modalNotifier = Registry.impl(ClientNotifications.class)
				.getModalNotifier("Restoring local database dump");
		String key = AlcinaHistory.get().getCurrentEvent()
				.getStringParameter(ALCINA_RESTORE_LOCAL_DB_KEY);
		final Callback afterRestoreCallback = new Callback() {
			@Override
			public void apply(Object value) {
				modalNotifier.modalOff();
				MessageManager.get()
						.centerMessage("Local database dump restored");
			}
		};
		final AsyncCallback<String> loadCallback = new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}

			@Override
			public void onSuccess(String result) {
				LocalTransformPersistence.get().restoreDatabase(result,
						afterRestoreCallback);
			}
		};
		((RemoteServiceProvider<? extends CommonRemoteServiceAsync>) Registry
				.impl(CommonRemoteServiceAsyncProvider.class))
						.getServiceInstance().loadData(key, loadCallback);
	}
}
