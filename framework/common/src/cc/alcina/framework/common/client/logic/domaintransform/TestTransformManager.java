package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class TestTransformManager extends ClientTransformManager {
	public void performDeleteObject(HasIdAndLocalId hili) {
	}

	@Override
	protected void callRemotePersistence(WrapperPersistable persistableObject,
			AsyncCallback<Long> savedCallback) {
	}

	@Override
	protected void doCascadeDeletes(HasIdAndLocalId hili) {
		super.doCascadeDeletes(hili);
	}
}
