package cc.alcina.framework.entity.entityaccess.cache.mvcc;

public interface MvccObject<T> {
	default void __debugResolvedVersion__() {
		MvccObjectVersions<T> versions = __getMvccVersions__();
		if (versions != null) {
			versions.debugResolvedVersion();
		}
	}

	MvccObjectVersions<T> __getMvccVersions__();

	void __setMvccVersions__(MvccObjectVersions<T> mvccVersions__);
}
