package cc.alcina.extras.dev.console.test;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntityLocalPromotionTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	@Override
	protected void run0() throws Exception {
		Class<IG> groupClass = (Class<IG>) AlcinaPersistentEntityImpl
				.getImplementation(IGroup.class);
		Class<IU> userClass = (Class<IU>) AlcinaPersistentEntityImpl
				.getImplementation(IUser.class);
		long suffix = System.currentTimeMillis();
		IG createdGroup = Domain.create(groupClass);
		IU createdUser = Domain.create(userClass);
		createdGroup.setGroupName("testgroup-" + suffix);
		createdUser.setUserName("testuser-" + suffix);
		createdGroup.domain().addToProperty("memberUsers", createdUser);
		Transaction.commit();
		Preconditions.checkState(Domain.find(createdUser) == createdUser);
		IU testInstance = userClass.newInstance();
		testInstance.setId(createdUser.getId());
		testInstance.hashCode();
		Preconditions
				.checkState(testInstance.hashCode() == createdUser.hashCode());
	}
}
