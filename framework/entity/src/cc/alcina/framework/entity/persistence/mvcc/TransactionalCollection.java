package cc.alcina.framework.entity.persistence.mvcc;

/*
 * Marker interface - implies should not be shallow-cloned when projecting versioned objects (instead, just assign the original instance)
 */
public interface TransactionalCollection {
}
