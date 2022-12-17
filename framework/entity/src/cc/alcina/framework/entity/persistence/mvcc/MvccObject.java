package cc.alcina.framework.entity.persistence.mvcc;

/*
 * TODO - document why MvccObjectVersions subclasses, rather than subclasses of
 * this class, are used in TransactionalMap
 */
public interface MvccObject<T> {
	MvccObjectVersions<T> __getMvccVersions__();

	void __setMvccVersions__(MvccObjectVersions<T> mvccVersions__);
}
