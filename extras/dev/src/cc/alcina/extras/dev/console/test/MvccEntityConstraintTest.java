package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.mvcc.MvccTestEntity;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

/**
 * <h3>Done:</h3>
 * <ul>
 * <li>Test framework
 * <li>Main-class: this, super, field access
 * <li>Inner class: super private method access, super field access
 * <li>Inner class: constructor
 * 
 * </ul>
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityConstraintTest extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		DomainStore.writableStore().getMvcc()
				.testTransformer(MvccTestEntity.class, null);
	}
}
