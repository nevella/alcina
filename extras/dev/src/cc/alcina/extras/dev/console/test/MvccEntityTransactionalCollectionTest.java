package cc.alcina.extras.dev.console.test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalCollectionTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	transient Class<IG> groupClass = (Class<IG>) PersistentImpl
			.getImplementation(IGroup.class);

	transient Class<IU> userClass = (Class<IU>) PersistentImpl
			.getImplementation(IUser.class);

	transient private CountDownLatch txLatch;

	transient private CountDownLatch tx1Latch1;

	transient private CountDownLatch tx1Latch2;

	transient private CountDownLatch tx2Latch1;

	transient private String username;

	transient private long initialSize;

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					List<IU> users2 = Domain.stream(userClass)
							.sorted(EntityComparator.INSTANCE)
							.collect(Collectors.toList());
					long suffix = System.currentTimeMillis();
					List<IU> users1 = Domain.stream(userClass)
							.collect(Collectors.toList());
					if (users1.size() != Domain.stream(userClass).count()) {
						// issue with layer merge, most likely
					}
					Preconditions.checkArgument(
							users2.size() == Domain.stream(userClass).count());
					IG createdGroup = Domain.create(groupClass);
					IU createdUser = Domain.create(userClass);
					createdGroup.setGroupName("testgroup-" + suffix);
					createdUser.setUserName(username);
					if (Domain.stream(userClass).count() != initialSize + 1) {
						long count = Domain.stream(userClass).count();
					}
					List<IU> users3 = Domain.stream(userClass)
							.sorted(EntityComparator.INSTANCE)
							.collect(Collectors.toList());
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"non-committed-tx1: userClass.count()!=initialSize+1 :: "
									+ Domain.stream(userClass).count());
					tx1Latch1.countDown();
					Transaction.commit();
					tx1Latch2.countDown();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"committed-tx1: userClass.count()!=initialSize+1");
				} catch (Exception e) {
					notifyThreadException(e);
					throw WrappedRuntimeException.wrap(e);
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
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize,
							"non-committed-tx1: userClass.count()!=initialSize in tx2");
					Ax.err("tx2:%s", Domain.stream(userClass).count());
					List<IU> users2 = Domain.stream(userClass)
							.sorted(EntityComparator.INSTANCE)
							.collect(Collectors.toList());
					tx2Latch1.countDown();
					tx1Latch2.await();
					Ax.err("tx2:%s", Domain.stream(userClass).count());
					List<IU> users1 = null;
					try {
						int debug = 4;
						users1 = Domain.stream(userClass)
								.collect(Collectors.toList());
						List<IU> users = Domain.stream(userClass)
								.sorted(EntityComparator.INSTANCE)
								.collect(Collectors.toList());
					} catch (Exception e) {
						e.printStackTrace();
					}
					Ax.err("tx2:%s", Domain.stream(userClass).count());
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize,
							"committed-tx1: userClass.count()!=initialSize in tx2 (old tx)");
					Transaction.endAndBeginNew();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize + 1,
							"committed-tx1: userClass.count()!=initialSize+1 thread tx2 (post-committed-tx1 tx)");
				} catch (Exception e) {
					notifyThreadException(e);
					throw WrappedRuntimeException.wrap(e);
				} finally {
					Transaction.ensureEnded();
					txLatch.countDown();
				}
			}
		}.start();
	}

	@Override
	protected void run0() throws Exception {
		username = "moew" + System.currentTimeMillis() + "@nodomain.com";
		initialSize = Domain.stream(userClass).count();
		Ax.err("Initial size: %s", initialSize);
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		startTx1();
		startTx2();
		txLatch.await();
	}
}
