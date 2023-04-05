package cc.alcina.extras.dev.console.test;

import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalDeletionTest<IU extends Entity & IUser, IG extends Entity & IGroup>
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

	private transient IU createdUser;

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					tx1Latch1.countDown();
					tx2Latch1.await();
					String txUserName = createdUser.getUserName();
					Preconditions.checkState(txUserName != null);
					Domain.stream(userClass).forEach(u -> {
						if (u.getId() == createdUser.getId()) {
							int debug = 3;
						}
						DomainStore.writableStore().getCache()
								.get(u.toLocator());
						((IUser) u).getUserName();
					});
					Ax.out("OK: username still visible: %s", txUserName);
					tx1Latch2.countDown();
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
					createdUser.delete();
					Transaction.commit();
					tx2Latch1.countDown();
					tx1Latch2.await();
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
	protected void run1() throws Exception {
		username = "moew" + System.currentTimeMillis() + "@nodomain.com";
		initialSize = Domain.stream(userClass).count();
		Ax.err("Initial size: %s", initialSize);
		createdUser = Domain.create(userClass);
		createdUser.setUserName(username);
		Transaction.commit();
		// wait for vacuum
		Thread.sleep(100);
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		startTx1();
		startTx2();
		txLatch.await();
	}
}
