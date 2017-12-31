package cc.alcina.framework.gwt.client.action;

import java.io.Serializable;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

public class DtrSimpleAdminPersistenceAction
		extends RemoteActionWithParameters<DeltaApplicationRecord>
		implements Serializable {
	public DtrSimpleAdminPersistenceAction() {
		DeltaApplicationRecord wrapper = new DeltaApplicationRecord();
		if (GWT.isClient()) {
			ClientInstance clientInstance = ClientBase.getClientInstance();
			wrapper.setClientInstanceAuth(clientInstance.getAuth());
			wrapper.setClientInstanceId(clientInstance.getId());
			wrapper.setRequestId(
					Registry.impl(CommitToStorageTransformListener.class)
							.getLocalRequestId());
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
