package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 *
 * 
 *
 */
public class MvccEntityAllTransactionTests extends PerformerTask {
	@Override
	public void run() throws Exception {
		/*
		 * not a tx test, but throw in
		 */
		// new MvccEntityConstraintTest().run();
		new MvccLiSetTest().run();
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
		new MvccEntitySortedIndexTest3().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityLazyPropertyTest().run();
		Transactions.waitForAllToCompleteExSelf();
		new MvccEntityTransactionalLoadTest().run();
		Transactions.waitForAllToCompleteExSelf();
	}
}
