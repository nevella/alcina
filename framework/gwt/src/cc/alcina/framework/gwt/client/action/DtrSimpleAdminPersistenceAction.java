package cc.alcina.framework.gwt.client.action;

import java.io.Serializable;

import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.core.client.GWT;

public class DtrSimpleAdminPersistenceAction extends
		RemoteActionWithParameters<DTRSimpleSerialWrapper> implements
		Serializable {
	public DtrSimpleAdminPersistenceAction() {
		DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper();
		if (GWT.isClient()) {
			ClientInstance clientInstance = ClientLayerLocator.get()
					.getClientInstance();
			wrapper.setClientInstanceAuth(clientInstance.getAuth());
			wrapper.setClientInstanceId(clientInstance.getId());
			wrapper.setRequestId(ClientLayerLocator.get()
					.getCommitToStorageTransformListener().getLocalRequestId());
		}
		setParameters(wrapper);
	}

	@Override
	public String getDescription() {
		return "Submit user-generated DTEs";
	}

	@Override
	public String getDisplayName() {
		return "Submit user transforms";
	}
}
