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
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;

/**
 *
 * 
 *
 */
public class MvccEntityTransactionalLoadTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	transient private CountDownLatch txLatch;

	transient private CountDownLatch tx1Latch1;

	transient private CountDownLatch tx1Latch2;

	transient private CountDownLatch tx2Latch1;

	transient private long initialCount;

	transient private long deletedCount;

	transient private long addedCount;

	transient private long minDeletionId;

	private <E extends Entity & IUser> Class<E> getUserClass() {
		return (Class<E>) PersistentImpl.getImplementation(IUser.class);
	}

	private long getUsersSize() {
		long count1 = Domain.stream(getUserClass()).count();
		long count2 = Domain.size(getUserClass());
		Preconditions.checkState(count1 == count2);
		return count1;
	}

	@Override
	protected void run1() throws Exception {
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
		// horribly slow
		AlcinaChildRunnable.runInTransactionNewThread("Delete-created", () -> {
			Ax.out("Deleting...");
			Domain.stream(getUserClass()).filter(u -> u.getId() > minDeletionId)
					.forEach(Entity::delete);
			Transaction.commit();
			Ax.out("Deleted");
		});
	}

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					int addCount = 10;
					int deleteCount = 5;
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
					stack();
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

			protected void stack() throws InterruptedException {
				Thread.sleep(30);
				List<Entity> collect = Domain.stream(getUserClass())
						.collect(Collectors.toList());
				Preconditions.checkState(getUsersSize() == initialCount,
						"non-committed-tx2: entities visible from tx2");
			}
		}.start();
	}
}
