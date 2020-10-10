/**
 * 
 * <h2>Transactional object</h2>
 * <ul>
 * <li>Every object implements MvccObject
 * <li>Every object has an MvccObjectVersions getter/setter (accessible via the
 * interface)
 * </ul>
 * 
 * 
 * <h2>MvccObject identity and debugging gotchas</h2>
 * <ul>
 * <li>An mvccobject (transactional entity) has essentially two roles: a store
 * of field values, and a 'domain identity'
 * <li>The 'domain identity' object is always the first instance of a given
 * entity (class/id or class/localid tuple) visible to the domain, and remains
 * the same object for the lifetime of the domain (i.e webapp or jvm). Even when
 * an entity is persisted to the db and assigned an id value, this relationship
 * holds (so there's no need to call entity.domain().domainVersion() after
 * persisting).
 * <li>If an entity has not been changed in any unvacuumed transaction,
 * MvccObjectVersions will be null and the property values returned from
 * getters/setters will be the field values of the 'domain identity' object.
 * <li>If an entity _has_ been changed, all visible methods will route to the
 * appropriate versioned instance ( which acts as a container of field values
 * for the transaction). There are some wrinkles to this logic (@see
 * MvccAccessType) but not for getters/setters.
 * <li>TLDR; For debugging, to view the field values of an object you're writing
 * to (if it has a non-zero id value), look at:
 * entity.__mvccObjectVersions__.__mostRecentWritable
 * </ul>
 * 
 * 
 * <h2>Transaction application (db tx -> domain store) (Nick's thoughts)</h2>
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
 * 
 */
package cc.alcina.framework.entity.persistence.mvcc;