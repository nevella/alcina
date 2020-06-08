package cc.alcina.extras.dev.console.test;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.TrieProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreDescriptor.TestSupport;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalTrieTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	private CountDownLatch txLatch;

	private CountDownLatch tx1Latch1;

	private CountDownLatch tx1Latch2;

	private CountDownLatch tx2Latch1;

	private String key;

	private TrieProjection projection;

	private String testKey;

	private long initialCount;

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					long suffix = System.currentTimeMillis();
					Registry.impl(TestSupport.class)
							.createTrieEntityInstance(key);
					long count = projection.getSubstringMatches(testKey)
							.count();
					Preconditions.checkState(count == initialCount + 1,
							"non-committed-tx1: index not visible from tx1");
					tx1Latch1.countDown();
					Transaction.commit();
					tx1Latch2.countDown();
					count = projection.getSubstringMatches(testKey).count();
					Preconditions.checkState(count == initialCount + 1,
							"committed-tx1: index not visible from tx1");
				} catch (Exception e) {
					notifyThreadException(e);
					throw new WrappedRuntimeException(e);
				} finally {
					Transaction.ensureEnded();
					txLatch.countDown();
				}
			}
		}.start();
	}

	private void startTx2() {
		new Thread("test-mvcc-2") {
			@Override
			public void run() {
				try {
					Transaction.begin();
					tx1Latch1.await();
					Object collect = projection.getSubstringMatches(testKey)
							.collect(Collectors.toList());
					long count = projection.getSubstringMatches(testKey)
							.count();
					Preconditions.checkState(count == initialCount,
							"non-committed-tx1: index visible from tx2");
					tx2Latch1.countDown();
					tx1Latch2.await();
					count = projection.getSubstringMatches(testKey).count();
					Preconditions.checkState(count == initialCount,
							"committed-tx1: index visible from tx2 (old tx)");
					Transaction.endAndBeginNew();
					count = projection.getSubstringMatches(testKey).count();
					Preconditions.checkState(count == initialCount + 1,
							"committed-tx1: index not visible from thread tx2 (post-committed-tx1 tx)");
				} catch (Exception e) {
					notifyThreadException(e);
					throw new WrappedRuntimeException(e);
				} finally {
					Transaction.ensureEnded();
					txLatch.countDown();
				}
			}
		}.start();
	}

	@Override
	protected void run0() throws Exception {
		projection = Registry.impl(TestSupport.class).getTrieProjection();
		key = "jajajamoew" + System.currentTimeMillis() + "@nodomain.com";
		testKey = "moew";
		initialCount = projection.getSubstringMatches(testKey).count();
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		startTx1();
		startTx2();
		txLatch.await();
	}
}
