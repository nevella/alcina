package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * Severe issues because MostlySingleValuedSet doesn't have any idea of
 * transactions - so say single-valued in base layer gets clobbered by first
 * write in a different tx
 * 
 * It might be possible to resuscitate - with the new txmap - but it may not be
 * worth the effort
 * 
 * ...furthermore, if there's a use case for single-valued-dom, it probably
 * makes more sense to put that in txmap
 */
public class TransactionalSetOld<E extends Entity> {
	// extends MostlySingleValuedSet<E>
	// implements MvccObject<TransactionalSetOld>, TransactionalCollection {
	// private Class<E> entityClass;
	//
	// MvccObjectVersions<TransactionalSetOld> __mvccObjectVersions__;
	//
	// public TransactionalSetOld(Class<E> entityClass) {
	// this.entityClass = entityClass;
	// }
	//
	// // for copying
	// TransactionalSetOld() {
	// }
	//
	// @Override
	// public MvccObjectVersions<TransactionalSetOld> __getMvccVersions__() {
	// return __mvccObjectVersions__;
	// }
	//
	// @Override
	// public void __setMvccVersions__(
	// MvccObjectVersions<TransactionalSetOld> __mvccVersions__) {
	// this.__mvccObjectVersions__ = __mvccVersions__;
	// }
	//
	// @Override
	// public boolean add(E o) {
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, true);
	// if (__instance__ == this) {
	// return super.add(o);
	// } else {
	// return __instance__.add(o);
	// }
	// }
	//
	// @Override
	// public Object clone() {
	// if (__mvccObjectVersions__ == null) {
	// return super.clone();
	// }
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, false);
	// if (__instance__ == this) {
	// return super.clone();
	// } else {
	// return __instance__.clone();
	// }
	// }
	//
	// @Override
	// public boolean contains(Object o) {
	// if (__mvccObjectVersions__ == null) {
	// return super.contains(o);
	// }
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, false);
	// if (__instance__ == this) {
	// return super.contains(o);
	// } else {
	// return __instance__.contains(o);
	// }
	// }
	//
	// @Override
	// public Iterator<E> iterator() {
	// if (__mvccObjectVersions__ == null) {
	// return super.iterator();
	// }
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, false);
	// if (__instance__ == this) {
	// return super.iterator();
	// } else {
	// return __instance__.iterator();
	// }
	// }
	//
	// public Class<E> entityClass() {
	// return entityClass;
	// }
	//
	// @Override
	// public boolean remove(Object o) {
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, true);
	// if (__instance__ == this) {
	// return super.remove(o);
	// } else {
	// return __instance__.remove(o);
	// }
	// }
	//
	// @Override
	// public int size() {
	// if (__mvccObjectVersions__ == null) {
	// return super.size();
	// }
	// TransactionalSetOld<E> __instance__ = Transactions
	// .resolveTransactionalSet(this, false);
	// if (__instance__ == this) {
	// return super.size();
	// } else {
	// return __instance__.size();
	// }
	// }
	//
	// @Override
	// protected Map<E, Boolean> createDegenerateMap() {
	// return new TransactionalMap<>(entityClass, Boolean.class);
	// }
}
