package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class TestTransformManager extends ClientTransformManager {
	public TestTransformManager() {
		createObjectLookup();
	}

	public static TestTransformManager cast() {
		return (TestTransformManager) TransformManager.get();
	}

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

	public List<DomainTransformEvent> transformInterceptList = null;

	@Override
	public void addTransform(DomainTransformEvent evt) {
		if (transformInterceptList != null) {
			transformInterceptList.add(evt);
		} else {
			super.addTransform(evt);
		}
	}

	@Override
	public void clearTransforms() {
		if (transformInterceptList != null) {
			transformInterceptList.clear();
		} else {
			super.clearTransforms();
		}
	}
}
