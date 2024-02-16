package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.MvccTestEntity;
import cc.alcina.framework.servlet.schedule.PerformerTask;

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
 * 
 *
 */
public class MvccEntityConstraintTest extends PerformerTask {
	@Override
	public void run() throws Exception {
		DomainStore.writableStore().getMvcc()
				.testTransformer(MvccTestEntity.class, null);
	}
}
