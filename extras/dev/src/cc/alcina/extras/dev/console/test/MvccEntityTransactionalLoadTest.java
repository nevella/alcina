package cc.alcina.extras.dev.console.test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalLoadTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	private CountDownLatch txLatch;

	private CountDownLatch tx1Latch1;

	private CountDownLatch tx1Latch2;

	private CountDownLatch tx2Latch1;

	private long initialCount;

	private long deletedCount;

	private long addedCount;

	private long minDeletionId;

	private <E extends Entity & IUser> Class<E> getUserClass() {
		return (Class<E>) AlcinaPersistentEntityImpl
				.getImplementation(IUser.class);
	}

	private long getUsersSize() {
		return Domain.stream(getUserClass()).count();
	}

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					int addCount = 120;
					int deleteCount = 10;
					addedCount = 0;
					for (int idx = 0; idx < addCount; idx++) {
						IUser user = Domain.create(getUserClass());
						user.setUserName(Ax.format(
								"MvccEntityTransactionalLoadTest-%s@alcina.cc",
								System.currentTimeMillis() + Math.random()));
						addedCount++;
					}
					long usersSize = getUsersSize();
					double filterProbability = ((double) deleteCount)
							/ usersSize;
					List<Entity> toDelete = Domain.stream(getUserClass())
							.filter(u -> (u.getId() > minDeletionId
									|| u.getId() == 0)
									&& Math.random() < filterProbability)
							.collect(Collectors.toList());
					toDelete.forEach(Entity::delete);
					deletedCount = toDelete.size();
					Preconditions.checkState(
							getUsersSize() == initialCount + addedCount
									- deletedCount,
							"non-committed-tx1: count not equal from tx1");
					tx1Latch1.countDown();
					Transaction.commit();
					tx1Latch2.countDown();
					Preconditions.checkState(
							getUsersSize() == initialCount + addedCount
									- deletedCount,
							"committed-tx1: count not equal from tx1");
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
					Preconditions.checkState(getUsersSize() == initialCount,
							"non-committed-tx2: entities visible from tx2");
					tx2Latch1.countDown();
					tx1Latch2.await();
					Transaction.endAndBeginNew();
					Preconditions.checkState(
							getUsersSize() == initialCount + addedCount
									- deletedCount,
							"committed-tx2: count not equal from tx1");
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
		minDeletionId = Domain.stream(getUserClass()).map(HasId::getId)
				.max(Comparator.naturalOrder()).get();
		for (int idx = 0; idx < 5; idx++) {
			initialCount = getUsersSize();
			Ax.sysLogHigh("Iteration: %s - intial count: %s", idx,
					initialCount);
			txLatch = new CountDownLatch(2);
			tx1Latch1 = new CountDownLatch(1);
			tx1Latch2 = new CountDownLatch(1);
			tx2Latch1 = new CountDownLatch(1);
			startTx1();
			startTx2();
			txLatch.await();
			Transaction.endAndBeginNew();
		}
	}
}
