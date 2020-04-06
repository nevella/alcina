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
 * <h2>Notes/think</h2>
 * <ul>
 * <li>Get rid of baselayerlist (transactionalmap)(special case the code)
 * <li>Use baseobject more in mvccversions (want to eventually have no ref to
 * base transaction)
 * <li>(Pre-jade) Cherry-pick memory usage to alcina.master. Compare memory
 * usage of a segment using propertystore vs regular objects for citaions/ccs.
 * Make framework-level 'populate DomainStoreProperty' - fingers crossed re
 * propertystore abandonment
 * </ul>
 * 
 * <h2>Versions</h2>
 * <h3>0.1</h3>
 * <h4>Goals</h4>
 * <ul>
 * <li>Have transactionally independent field values for all domain objects
 * <li>Have 'one unique object' for the lifetime of the vm - including when
 * promoted from 'local' to 'domain'
 * <li>Have relatively proveable zero-information leakage of transactional
 * version identity data (i.e. the unique-to-tx entity objects do not leak this
 * - and thereby hashcode/equals - outside their own code)
 * </ul>
 * <h4>Nice to have</h4>
 * <ul>
 * <li>Minimal memory use increase - or even lower it
 * <li>More performant transform request communication for cache change
 * propogation
 * 
 * </ul>
 * <h4>Non-goals</h4>
 * <ul>
 * <li>Rework the logging system
 * <li>Rework the jobs system
 * <li>Refactor DomainStore more than needed
 * </ul>
 * <h2>TODO</h2>
 * <ul>
 * <li>Facade class code creation (yep)
 * <li>Facade object creation (domain.create => mvcc.create) (yep)
 * <li>MvMaps (domaincache; indicies)(yep)
 * <li>Transaction phase management (local; remote)(vacuum) (yap)
 * <p>
 * This is actually easyish - a local transaction can be aborted (unsupp for the
 * mo) - a db-committed
 * </p>
 * <li>Graph projection
 * <li>Remove locks (make em just notational)(altho post-process remains
 * synchronized)
 * <li>Post-process and transaction cleanup. Delete cascade can be a lot less
 * brutal (just associations)
 * <li>Check no field assignment within entity private methods, and no calls to
 * setters (pretty sure checked as part of bytecode generation)
 * <li>Tx abort - check hits vacuum etc
 * </ul>
 * <h2>Vacuum</h2>
 * <ul>
 * <li>MvccObject - just remove invisible versions
 * <li>Transactional map - make new, squash, hot swap (10x 'levelled compaction'
 * scaling down)
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
 * <li>Should transactionid be used more?
 * <li>Divide transaction phases into categories
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