package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.List;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class TestTransformManager extends ClientTransformManager {
	public static TestTransformManager cast() {
		return (TestTransformManager) TransformManager.get();
	}

	public List<DomainTransformEvent> transformInterceptList = null;

	public TestTransformManager() {
		createObjectLookup();
	}

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

	@Override
	public void performDeleteObject(Entity entity) {
	}
}
