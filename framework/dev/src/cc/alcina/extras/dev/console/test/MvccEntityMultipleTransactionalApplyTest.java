package cc.alcina.extras.dev.console.test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.ResolvedVersionState;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;

/**
 * 
 * 
 *
 */
public class MvccEntityMultipleTransactionalApplyTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	transient Class<IG> groupClass = (Class<IG>) PersistentImpl
			.getImplementation(IGroup.class);

	transient Class<IU> userClass = (Class<IU>) PersistentImpl
			.getImplementation(IUser.class);

	transient private CountDownLatch txLatch;

	transient private CountDownLatch tx1Latch1;

	transient private CountDownLatch tx1Latch2;

	transient private CountDownLatch tx2Latch1;

	transient private CountDownLatch tx2Latch2;

	transient private CountDownLatch tx3Latch1;

	transient private long initialSize;

	transient private IG createdGroup;

	transient private IU createdUser1;

	transient private IU createdUser2;

	transient private IU createdUser3;

	@Override
	protected void run1() throws Exception {
		txLatch = new CountDownLatch(3);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		tx2Latch2 = new CountDownLatch(1);
		tx3Latch1 = new CountDownLatch(1);
		long suffix = System.currentTimeMillis();
		createdGroup = Domain.create(groupClass);
		createdGroup.setGroupName("testgroup-" + suffix);
		String username1 = "moew1" + System.currentTimeMillis()
				+ "@nodomain.com";
		String username2 = "moew2" + System.currentTimeMillis()
				+ "@nodomain.com";
		String username3 = "moew3" + System.currentTimeMillis()
				+ "@nodomain.com";
		createdUser1 = Domain.create(userClass);
		createdUser2 = Domain.create(userClass);
		createdUser3 = Domain.create(userClass);
		createdUser1.setUserName(username1);
		createdUser2.setUserName(username2);
		createdUser3.setUserName(username3);
		initialSize = 0;
		Transaction.commit();
		Transaction.end();
		Thread.sleep(100);
		startTx1();
		startTx2();
		startTx3();
		txLatch.await();
		Transaction.begin();
	}

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					createdGroup.domain().addToProperty("memberUsers",
							createdUser1);
					tx1Latch1.countDown();
					tx2Latch1.await();
					tx3Latch1.await();
					Preconditions.checkState(
							createdGroup.getMemberUsers().size() == initialSize
									+ 1,
							"not-committed-tx1: createdGroup.getMemberUsers().size()!=initialSize+1");
					Transaction.commit();
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
					Transaction.ensureBegun();
					createdGroup.domain().addToProperty("memberUsers",
							createdUser2);
					IG resolveFalse = Transactions.resolve(createdGroup,
							ResolvedVersionState.READ, false);
					IG resolveTrue = Transactions.resolve(createdGroup,
							ResolvedVersionState.WRITE, false);
					tx2Latch1.countDown();
					tx3Latch1.await();
					// this one to avoid a concurrent db (hibernate) mod
					tx1Latch2.await();
					IG resolve3 = Transactions.resolve(createdGroup,
							ResolvedVersionState.READ, false);
					if (createdGroup.getMemberUsers().size() != initialSize
							+ 1) {
						Set<? extends IUser> memberUsers = createdGroup
								.getMemberUsers();
						long size2 = memberUsers.stream().count();
					}
					Preconditions.checkState(
							createdGroup.getMemberUsers().size() == initialSize
									+ 1,
							Ax.format(
									"not-committed-tx2: createdGroup.getMemberUsers().size()!=initialSize+1 : ",
									createdGroup.getMemberUsers().size()));
					Transaction.commit();
					tx2Latch2.countDown();
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

	private void startTx3() {
		new Thread("test-mvcc-3") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					createdGroup.domain().addToProperty("memberUsers",
							createdUser3);
					tx3Latch1.countDown();
					tx1Latch2.await();
					tx2Latch2.await();
					Set<? extends IUser> memberUsers = createdGroup
							.getMemberUsers();
					long size2 = memberUsers.stream().count();
					Preconditions.checkState(
							createdGroup.getMemberUsers().size() == initialSize
									+ 1,
							"not-committed-tx3: createdGroup.getMemberUsers().size()!=initialSize+1");
					Transactions.pauseVacuum(true);
					Transaction.commit();
					Set<? extends IUser> shouldBeThree = createdGroup
							.getMemberUsers();
					Transactions.pauseVacuum(false);
					Transactions.waitForAllToCompleteExSelf();
					Set<? extends IUser> shouldBeThree2 = createdGroup
							.getMemberUsers();
					Preconditions.checkState(
							createdGroup.getMemberUsers().size() == initialSize
									+ 3,
							"committed-tx3 (and tx1,tx2): createdGroup.getMemberUsers().size()!=initialSize+3");
					// vacuum
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
}
