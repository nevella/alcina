package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.mvcc.MvccTestEntity;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class MvccEntityConstraintTest extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		DomainStore.writableStore().getMvcc()
				.testTransformer(MvccTestEntity.class);
	}
}
