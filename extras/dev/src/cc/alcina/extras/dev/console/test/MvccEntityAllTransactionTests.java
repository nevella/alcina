package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transactions;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityAllTransactionTests extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		new MvccEntityLocalPromotionTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityTransactionalIndexTest().run();
		Thread.sleep(1000);
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityTransactionalCollectionTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityDeletionPropagationTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityMultipleTransactionalApplyTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntitySortedIndexTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntitySortedIndexTest2().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityLazyPropertyTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityTransactionalLoadTest().run();
		Transactions.waitForAllToCompleteExSelf();
	}
}
