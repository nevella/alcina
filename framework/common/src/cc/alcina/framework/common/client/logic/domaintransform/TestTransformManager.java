package cc.alcina.framework.common.client.logic.domaintransform;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class TestTransformManager extends ClientTransformManager {
	public void performDeleteObject(HasIdAndLocalId hili) {
	}

	@Override
	protected void callRemotePersistence(WrapperPersistable persistableObject,
			AsyncCallback<Long> savedCallback) {
		// TODO Auto-generated method stub
		
	}
}
