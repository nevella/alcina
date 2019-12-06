/**
 * <h2>Transactional object</h2>
 * <ul>
 * <li>Every object implements MvccObject
 * <li>Every object has an MvccObjectTransactions field (accessible via the
 * interface)
 * </ul>
 * 
 * <h2>Transaction application</h2>
 * <ul>
 * <li>Transactions application is sequential, in db order
 * <li>Strategies to reduce application times...?
 * <li>...maybe have a speculative apply which can be rewound if collisions
 * occur. Only issue is indexing but most indicies - if not totally derived from
 * non-domain object fields - are derived from unchanging domain references
 * <li>Aha! When publishing (kafka) "persisting tx id", publish row-level
 * 'locks' (class.id tuples modified). If no conflicts, tx can be applied
 * out-of-order (to the object)
 * <li>application-level question about whether, then, we should wait for all
 * txs with prior db commit times
 * <li>Anyway, that's for the future - for the moment stick with sequential
 * commit and reduce time there where possible
 * <li>And then of course there's ... eventual consistency
 * </ul>
 * 
 * <h2>TODO</h2>
 * <ul>
 * <li>Facade class code creation (yep)
 * <li>Facade object creation (domain.create => mvcc.create) (yep)
 * <li>MvMaps (domaincache; indicies)(yep)
 * <li>Transaction phase management (local; remote)
 * <p>
 * This is actually easyish - a local transaction can be aborted (unsupp for the
 * mo) - a db-committed
 * </p>
 * <li>Graph projection
 * <li>Remove locks (make em just notational)(altho post-process remains
 * synchronized)
 * <li>Post-process and transaction cleanup
 * <li>Check no field assignment within hili private methods, and no calls to
 * setters (pretty sure checked as part of bytecode generation)
 * <li>Tx abort - devconsole - write backup version fields to base object (see
 * MvccObjectVersions constructor). Tx finish - remove tltm listeners. Make tltm
 * listeners thread-specific (actually no - that would only be needed for
 * devconsole, since non-dc listeners always ref unique-to-tltm object versions)
 * </ul>
 * *
 * <h2>Tests (1)</h2>
 * <ul>
 * <li>Does the transactionalmap/layer/iterator work as intended? Any possible
 * multithreaded races??
 * <li>Does post-process work (devconsole) - do we get new obj versions derived
 * from correct version
 * <li>Does post-process work (devconsole) - ditto lookups
 * <li>Does post-process work (devconsole) - modify an object in two
 * transactions
 * <li>Do UI tree and collection UI selectors work?
 * </ul>
 * <h2>TODO - post</h2>
 * <ul>
 * <li>fix non-safety of method comparisonString() (by removing - this
 * comparator shd be client only - have a real, caching comparator)
 * <li>Associations! General plan!
 * <li>Associations! General plan! (welll...we have one. write without
 * transforms, both server and client side, on post_process and on transform
 * receipt)
 * <li>Clone code creation
 * <li>Vacuum
 * <li>DirectAccess annotation should have a hash (of the method source)
 * <li>
 * </ul>
 */
package cc.alcina.framework.entity.entityaccess.cache.mvcc;