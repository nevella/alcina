package cc.alcina.extras.dev.console.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasDate;
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
 *         In one thread, create 100 date entities [thread 1]
 *
 *         Then, randomly modify and commit [thread 1]
 *
 *         Simultaneously, randomly access and get [thread 2]
 *
 *         Repeat n times
 *
 *         Check result is consistent with thread 1 changes
 *
 */
public class MvccEntitySortedIndexTest3<IU extends Entity & IUser, IG extends Entity & IGroup>
		extends MvccEntityTransactionTest {
	transient ConcurrentHashMap<Entity, Date> entityDates = new ConcurrentHashMap<>();

	transient List<Entity> entities = new ArrayList<>();

	transient CountDownLatch creationCompleteLatch = new CountDownLatch(1);

	transient CountDownLatch modificationCompleteLatch = new CountDownLatch(1);

	transient CountDownLatch thread1CompletedLatch = new CountDownLatch(1);

	transient private Thread0 thread0;

	transient private Thread1 thread1;

	private void checkDates() {
		entityDates.forEach((e, d) -> {
			Preconditions.checkState(((HasDate) e).getDate().equals(d));
		});
	}

	private void deleteEntities() {
		entities.forEach(Entity::delete);
		Transaction.commit();
	}

	private Entity randomEntity() {
		int idx = (int) Math.random() * entities.size();
		return entities.get(idx);
	}

	@Override
	protected void run0() throws Exception {
		Transaction.ensureEnded();
		thread0 = new Thread0();
		thread1 = new Thread1();
		thread0.start();
		thread1.start();
		thread1CompletedLatch.await();
		Transaction.ensureBegun();
		checkDates();
		deleteEntities();
	}

	class Thread0 extends Thread {
		public Thread0() {
			setName("MvccEntitySortedIndexTest3-Thread0");
		}

		@Override
		public void run() {
			try {
				Transaction.begin();
				createEntities();
				creationCompleteLatch.countDown();
				mutateEntities();
				modificationCompleteLatch.countDown();
			} finally {
				Transaction.ensureEnded();
			}
		}

		private void createEntities() {
			for (int idx = 0; idx < 200; idx++) {
				Entity entity = Registry.impl(TestSupport.class)
						.createReversedDateEntityInstance();
				setDate(entity);
				entities.add(entity);
				if (Math.random() < 0.2) {
					setDate(entity);
				}
			}
			Transaction.commit();
		}

		private void mutateEntities() {
			for (int txIdx = 0; txIdx < 40; txIdx++) {
				for (int idx = 0; idx < 20; idx++) {
					Entity entity = randomEntity();
					setDate(entity);
				}
				Transaction.commit();
				logger.info("Committed mutation transaction: {}", txIdx);
			}
		}

		private void setDate(Entity entity) {
			Date date = new Date((long) (Math.random() * 100000));
			((HasDate) entity).setDate(date);
			entityDates.put(entity, date);
		}
	}

	class Thread1 extends Thread {
		int counter = 0;

		public Thread1() {
			setName("MvccEntitySortedIndexTest3-Thread1");
		}

		@Override
		public void run() {
			try {
				creationCompleteLatch.await();
				while (true) {
					Transaction.ensureBegun();
					HasDate randomEntity = (HasDate) randomEntity();
					Date date = randomEntity.getDate();
					if (counter++ % 100 == 0) {
						Ax.out("reader - %s - %s - %s", counter, randomEntity,
								date);
						Transaction.endAndBeginNew();
					}
					Thread.sleep(0, 1000);
					if (modificationCompleteLatch.getCount() <= 0) {
						thread1CompletedLatch.countDown();
						break;
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				Transaction.ensureEnded();
			}
		}
	}
}
