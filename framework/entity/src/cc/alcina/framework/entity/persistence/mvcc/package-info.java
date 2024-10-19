/**
 * <p>
 * (v1) See the {@link cc.alcina.framework.entity.persistence.mvcc.Mvcc} class
 * for an overview of the Alcina Mvcc system
 * 
 * <p>
 * (v2)This component implements MVCC transactions and transactional objects for
 * the Domain component. Some of the transactional id terminology is borrowed
 * from the postgresql project's mvcc system, but there are few other
 * similarities (since this is an MVCC object, not relational database system).
 * 
 * 
 * <p>
 * The domain/mvcc subsystem provides high-performance transactional views of
 * domain objects (entities, indicies, tries). The implementation is biased
 * towards read-heavy workloads - when an entity has not been changed during a
 * non-vacuumed transaction, the extra cost of any public entity method call is
 * a single null check - i.e. extremely low.
 * 
 * <p>
 * When an entity has been changed, each changed version of the entity is
 * modelled by a new instance of the entity class. Those instances act
 * essentially as stores of values - specifically code analysis disallows usage
 * of the version instance's identity - the keyword this cannot be used in
 * transactional entity code, instead the {@link Entity#domainIdentity} method
 * is used to access the object identity of the entity. This ensures that there
 * is only one 'identity' instance of an entity visible to application code, and
 * that object versions cannot leak across transaction bounrdaries.
 * 
 * <p>
 * Routing and maintenance of versions is controlled by a per-entity
 * {@link MvccObjectVersions} object.
 * 
 * 
 * <h4>Example of transactional entity access and modification</h4>
 * 
 * <pre>
 * <code>
 * 
 class Test {
	Pastry pastry;

	CountDownLatch thread2commitAwait = new CountDownLatch(1);

	void run() {
		pastry = Pastry.create();
		pastry.setName("Ensaimada");
		pastry.setUnitPrice(2.5);
		Transaction.commit();
		new Thread("thread-1") {
			&#64;Override
			public void run() {
				try {
					Transaction.ensureBegun();
					//
						// wait until thread2 completes (note that the current
						// tx will have started before t2's )
						//
					thread2commitAwait.await();
					// do *not* start a new transaction -
					// Transaction.endAndBeginNew();
					// will be 2.5
					Ax.out("%s - %a", Thread.currentThread().getName(),
							pastry.getUnitPrice());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		new Thread("thread-2") {
			&#64;Override
			public void run() {
				Transaction.begin();
				pastry.setUnitPrice(3.5);
				Transaction.commit();
				thread2commitAwait.countDown();
				// will be 3.5
				Ax.out("%s - %a", Thread.currentThread().getName(),
						pastry.getUnitPrice());
				Transaction.end();
			}
		}.start();
		new Thread("thread-3") {
		&#64;Override
			public void run() {
				try {
					thread2commitAwait.await();
					// start a new transaction to see the committed value
					// from thread 1
					Transaction.begin();
					// will be 3.5
					Ax.out("%s - %a", Thread.currentThread().getName(),
							pastry.getUnitPrice());
					Transaction.end();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
 * </code>
 * </pre>
 * 
 * 
 * <p>
 * Exampe of rewritten code (TODO)
 * 
 * 
 * <h4>Diagrams:</h4>
 * <ul>
 * <li>Version creation and vacuum sequence
 * </ul>
 * 
 * <h4>Major classes in the system:</h4>
 * 
 * <ul>
 * <li>Vacuum
 * <li>TransactonalMap
 * <li>Trie
 * <li>MvccObjectVersions
 * <li>[Domain, Transactions, Transaction, TLTM, Entity, ObjectVersions,
 * ObjectVersion] </ul
 * <p>
 * Catechism: *
 * <p>
 * General implementation notes: Some of the transactional id terminology is
 * borrowed from the postgresql project's mvcc system, but there are few other
 * similarities (since this is an MVCC object, not relational database system).
 */
package cc.alcina.framework.entity.persistence.mvcc;
