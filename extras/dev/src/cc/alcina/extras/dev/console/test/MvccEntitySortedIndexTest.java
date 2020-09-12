package cc.alcina.extras.dev.console.test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.ReverseDateProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreDescriptor.TestSupport;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntitySortedIndexTest<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	private void debug(List<Entity> list) {
		Ax.out(list.stream().map(Entity::toLocator)
				.collect(Collectors.toList()));
	}

	@Override
	protected void run0() throws Exception {
		ReverseDateProjection projection = Registry.impl(TestSupport.class)
				.getReversedDateProjection();
		Date date = new Date();
		Entity instance = Registry.impl(TestSupport.class)
				.createReversedDateEntityInstance();
		Preconditions.checkState(projection.getSince(date).size() == 1);
		debug(projection.getSince(date));
		Transaction.commit();
		instance = Registry.impl(TestSupport.class)
				.createReversedDateEntityInstance();
		Preconditions.checkState(projection.getSince(date).size() == 2);
		debug(projection.getSince(date));
		instance = Registry.impl(TestSupport.class)
				.createReversedDateEntityInstance();
		Preconditions.checkState(projection.getSince(date).size() == 3
				&& projection.getSince(date).get(0) == instance);
		debug(projection.getSince(date));
		Transaction.commit();
		Preconditions.checkState(projection.getSince(date).size() == 3
				&& projection.getSince(date).get(0) == instance);
		debug(projection.getSince(date));
	}
}
