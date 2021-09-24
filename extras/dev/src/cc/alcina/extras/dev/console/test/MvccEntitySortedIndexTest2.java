package cc.alcina.extras.dev.console.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.ReverseDateProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDescriptor.TestSupport;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class MvccEntitySortedIndexTest2<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	transient List<Entity> entitiesInCreationOrder = new ArrayList<>();

	transient Entity lastInstance;

	private void debug(List<Entity> list) {
		if (lastInstance == null) {
			lastInstance = Ax.first(list);
		} else {
			Preconditions.checkArgument(
					Objects.equals(lastInstance, Ax.first(list)));
		}
		Ax.out(list.stream().map(Entity::toLocator)
				.collect(Collectors.toList()));
	}

	@Override
	protected void run0() throws Exception {
		ReverseDateProjection projection = Registry.impl(TestSupport.class)
				.getReversedDateProjection();
		Date date = new Date();
		{
			Entity instance = Registry.impl(TestSupport.class)
					.createReversedDateEntityInstance();
			entitiesInCreationOrder.add(instance);
			Thread.sleep(3);
		}
		for (int idx = 0; idx < 100; idx++) {
			Entity instance = Registry.impl(TestSupport.class)
					.createReversedDateEntityInstance();
			entitiesInCreationOrder.add(instance);
		}
		Transaction.commit();
		List<Entity> list = Collections.singletonList(
				(Entity) projection.getSince(date).iterator().next());
		debug(list);
		Ax.out(list);
		Registry.impl(TestSupport.class).performReversedDateModification(
				entitiesInCreationOrder.get(0));
		Transaction.commit();
		Thread.sleep(100);
		list = Collections.singletonList(
				(Entity) projection.getSince(date).iterator().next());
		debug(list);
		Thread.sleep(1000);
		list = Collections.singletonList(
				(Entity) projection.getSince(date).iterator().next());
		debug(list);
		Entity instance = Registry.impl(TestSupport.class)
				.createReversedDateEntityInstance();
		// Check iterator length == actual length (after
		// change not
		// add-delete)
		Preconditions.checkState(Domain.size(instance.entityClass()) == Domain
				.stream(instance.entityClass()).count());
	}
}
