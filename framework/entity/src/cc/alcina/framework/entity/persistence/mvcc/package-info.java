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
 * <p>
 * Principal classes: [Domain, Transactions, Transaction, TLTM, Entity,
 * ObjectVersions, ObjectVersion]
 * 
 */
package cc.alcina.framework.entity.persistence.mvcc;