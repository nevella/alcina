package cc.alcina.extras.dev.console.test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalCollectionTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends AbstractTaskPerformer {
	Class<IG> groupClass = (Class<IG>) AlcinaPersistentEntityImpl
			.getImplementation(IGroup.class);

	Class<IU> userClass = (Class<IU>) AlcinaPersistentEntityImpl
			.getImplementation(IUser.class);

	private CountDownLatch txLatch;

	private CountDownLatch tx1Latch1;

	private CountDownLatch tx1Latch2;

	private CountDownLatch tx2Latch1;

	private String username;

	private long initialSize;

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					long suffix = System.currentTimeMillis();
					IG createdGroup = Domain.create(groupClass);
					IU createdUser = Domain.create(userClass);
					createdGroup.setGroupName("testgroup-" + suffix);
					createdUser.setUserName(username);
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"non-committed-tx1: userClass.count()!=initialSize+1");
					tx1Latch1.countDown();
					Sx.commit();
					tx1Latch2.countDown();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"committed-tx1: userClass.count()!=initialSize+1");
				} catch (Exception e) {
					txLatch.countDown();
					throw WrappedRuntimeException.wrapIfNotRuntime(e);
				} finally {
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
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize,
							"non-committed-tx1: userClass.count()!=initialSize in tx2");
					tx2Latch1.countDown();
					tx1Latch2.await();
					List<IU> users = Domain.stream(userClass)
							.sorted(EntityComparator.INSTANCE)
							.collect(Collectors.toList());
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize,
							"committed-tx1: userClass.count()!=initialSize in tx2 (old tx)");
					Transaction.endAndBeginNew();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"committed-tx1: userClass.count()!=initialSize+1 thread tx2 (post-committed-tx1 tx)");
				} catch (Exception e) {
					throw WrappedRuntimeException.wrapIfNotRuntime(e);
				} finally {
					txLatch.countDown();
				}
			}
		}.start();
	}

	@Override
	protected void run0() throws Exception {
		username = "moew" + System.currentTimeMillis() + "@nodomain.com";
		initialSize = Domain.stream(userClass).count();
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		startTx1();
		startTx2();
		txLatch.await();
	}
}
