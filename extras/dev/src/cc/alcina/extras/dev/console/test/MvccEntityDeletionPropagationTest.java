package cc.alcina.extras.dev.console.test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityDeletionPropagationTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
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

	private IG createdGroup;

	private IU createdUser;

	private void startTx1() {
		new Thread("test-mvcc-1") {
			@Override
			public void run() {
				try {
					Transaction.ensureBegun();
					Preconditions.checkState(
							createdGroup.containsUser(createdUser),
							"pre-delete: referenced group does not contain user");
					createdUser.delete();
					Domain.stream(userClass).count();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize - 1,
							"non-committed-tx1: userClass.count()!=initialSize-1");
					Set<? extends IUser> memberUsers = createdGroup
							.getMemberUsers();
					Preconditions.checkState(
							!createdGroup.containsUser(createdUser),
							"post-delete: referenced group contains user");
					IU createdUser2 = Domain.create(userClass);
					createdUser2
							.setUserName("will-delete-in-this-tx@nodomain.com");
					createdGroup.domain().addToProperty("memberUsers",
							createdUser2);
					createdUser2.delete();
					List<IU> users = Domain.stream(userClass)
							.collect(Collectors.toList());
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize - 1,
							"non-committed-tx1: userClass.count()!=initialSize-1");
					Preconditions.checkState(
							!createdGroup.containsUser(createdUser2),
							"post-delete: referenced group contains user2");
					tx1Latch1.countDown();
					Transaction.commit();
					tx1Latch2.countDown();
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize - 1,
							"committed-tx1: userClass.count()!=initialSize-1");
				} catch (Exception e) {
					notifyThreadException(e);
					throw WrappedRuntimeException.wrapIfNotRuntime(e);
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
							createdGroup.containsUser(createdUser),
							"non=committed pre-delete (tx2): referenced group does not contain user");
					test();
					tx2Latch1.countDown();
					tx1Latch2.await();
					Preconditions.checkState(
							createdGroup.containsUser(createdUser),
							"pre-delete (old tx2): referenced group does not contain user");
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize,
							"non-committed-tx2: userClass.count()!=initialSize");
					Transaction.endAndBeginNew();
					Preconditions.checkState(
							!createdGroup.containsUser(createdUser),
							"post-delete (tx2): referenced group contains user");
					Preconditions.checkState(
							Domain.stream(userClass).count() == initialSize - 1,
							"committed-tx1: userClass.count()!=initialSize-1 thread tx2 (post-committed-tx1 tx)");
				} catch (Exception e) {
					notifyThreadException(e);
					throw WrappedRuntimeException.wrapIfNotRuntime(e);
				} finally {
					Transaction.ensureEnded();
					txLatch.countDown();
				}
			}

			protected void test() {
				boolean check = Domain.stream(userClass).count() == initialSize;
				Preconditions.checkState(
						Domain.stream(userClass).count() == initialSize,
						"non-committed-tx2: userClass.count()!=initialSize");
			}
		}.start();
	}

	@Override
	protected void run0() throws Exception {
		username = "moew" + System.currentTimeMillis() + "@nodomain.com";
		txLatch = new CountDownLatch(2);
		tx1Latch1 = new CountDownLatch(1);
		tx1Latch2 = new CountDownLatch(1);
		tx2Latch1 = new CountDownLatch(1);
		long suffix = System.currentTimeMillis();
		createdGroup = Domain.create(groupClass);
		createdUser = Domain.create(userClass);
		createdGroup.setGroupName("testgroup-" + suffix);
		createdUser.setUserName(username);
		createdGroup.addMemberUser(createdUser);
		Transaction.commit();
		// HashSetExtension.debugInstance = (HashSetExtension) createdGroup
		// .getMemberUsers();
		initialSize = Domain.stream(userClass).count();
		startTx1();
		startTx2();
		txLatch.await();
	}
}
