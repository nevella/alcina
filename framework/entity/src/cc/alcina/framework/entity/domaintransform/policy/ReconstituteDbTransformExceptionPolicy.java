package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;

public class ReconstituteDbTransformExceptionPolicy extends
		IgnoreMissingPersistenceLayerTransformExceptionPolicy {
	@Override
	public int tooManyExceptions() {
		return 9999;
	}

	@Override
	public boolean precreateMissingEntities() {
		return true;
	}
}