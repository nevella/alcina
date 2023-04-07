package cc.alcina.extras.dev.console.test;

import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityTransactionalIndexTest<IU extends Entity & IUser, IG extends Entity & IGroup>
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
					createdGroup.domain().addToProperty("memberUsers",
							createdUser);
					IU foundUser = Domain.by(userClass, "email", username);
					Preconditions.checkState(foundUser != null,
							"non-committed-tx1: index not visible from tx1");
					tx1Latch1.countDown();
					Transaction.commit();
					tx1Latch2.countDown();
					foundUser = Domain.by(userClass, "email", username);
					Preconditions.checkState(foundUser != null,
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
					IU foundUser = Domain.by(userClass, "email", username);
					Preconditions.checkState(foundUser == null,
							"non-committed-tx1: index visible from tx2");
					tx2Latch1.countDown();
					tx1Latch2.await();
					foundUser = Domain.by(userClass, "email", username);
					Preconditions.checkState(foundUser == null,
							"committed-tx1: index visible from tx2 (old tx)");
					Transaction.endAndBeginNew();
					foundUser = Domain.by(userClass, "email", username);
					Preconditions.checkState(foundUser != null,
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
	protected void run1() throws Exception {
		username = "moew" + System.currentTimeMillis() + "@nodomain.com";
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		startTx1();
		startTx2();
		txLatch.await();
	}
}
