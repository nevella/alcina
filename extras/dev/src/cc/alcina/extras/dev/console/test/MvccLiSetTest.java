package cc.alcina.extras.dev.console.test;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class MvccLiSetTest extends PerformerTask {
	@Override
	public void run() throws Exception {
		Transaction.end();
		Ax.err(getClass().getSimpleName());
		Class<? extends ClientInstance> clazz = PersistentImpl
				.getImplementation(ClientInstance.class);
		for (int pass = 0; pass < 30; pass++) {
			LiSet<ClientInstance> set = new LiSet<>();
			for (int idx = 0; idx < 29; idx++) {
				boolean local = Math.random() > 0.5;
				long value = (long) (Math.random() * 1000000) + 1;
				ClientInstance newInstance = Reflections.newInstance(clazz);
				if (local) {
					newInstance.setLocalId(value);
					boolean andWithId = Math.random() > 0.5;
					if (andWithId) {
						long value2 = (long) (Math.random() * 1000000) + 1;
						newInstance.setId(value2);
					}
				} else {
					newInstance.setId(value);
				}
				set.add(newInstance);
			}
			List<ClientInstance> list1 = set.stream()
					.collect(Collectors.toList());
			List<ClientInstance> list2 = set.stream()
					.sorted(new LiSet.TestComparator())
					.collect(Collectors.toList());
			Preconditions.checkState(list1.equals(list2));
		}
		Transaction.begin();
	}
}
