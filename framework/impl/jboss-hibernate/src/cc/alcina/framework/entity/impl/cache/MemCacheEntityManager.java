package cc.alcina.framework.entity.impl.cache;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.Session;

import cc.alcina.framework.entity.ResourceUtilities;

public class MemCacheEntityManager implements EntityManager {
	private EntityManager delegate;

	public void clear() {
		this.delegate.clear();
	}

	public void close() {
		this.delegate.close();
	}

	public boolean contains(Object arg0) {
		return this.delegate.contains(arg0);
	}

	public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
		return this.delegate.createNamedQuery(arg0, arg1);
	}

	public Query createNamedQuery(String arg0) {
		return this.delegate.createNamedQuery(arg0);
	}

	public Query createNativeQuery(String arg0, Class arg1) {
		return this.delegate.createNativeQuery(arg0, arg1);
	}

	public Query createNativeQuery(String arg0, String arg1) {
		return this.delegate.createNativeQuery(arg0, arg1);
	}

	public Query createNativeQuery(String arg0) {
		return this.delegate.createNativeQuery(arg0);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		return this.delegate.createQuery(arg0);
	}

	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		return this.delegate.createQuery(arg0, arg1);
	}

	public Query createQuery(String arg0) {
		return this.delegate.createQuery(arg0);
	}

	public void detach(Object arg0) {
		this.delegate.detach(arg0);
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
			Map<String, Object> arg3) {
		return this.delegate.find(arg0, arg1, arg2, arg3);
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		return this.delegate.find(arg0, arg1, arg2);
	}

	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		return this.delegate.find(arg0, arg1, arg2);
	}

	public <T> T find(Class<T> arg0, Object arg1) {
		return this.delegate.find(arg0, arg1);
	}

	public void flush() {
		this.delegate.flush();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return this.delegate.getCriteriaBuilder();
	}

	public Object getDelegate() {
		Object subDelegate = this.delegate == null ? null : this.delegate
				.getDelegate();
		if (ResourceUtilities.is(MemCacheEntityManager.class, "useMemCache")) {
			return new MemCacheSession((Session) subDelegate);
		} else {
			return subDelegate;
		}
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return this.delegate.getEntityManagerFactory();
	}

	public FlushModeType getFlushMode() {
		return this.delegate.getFlushMode();
	}

	public LockModeType getLockMode(Object arg0) {
		return this.delegate.getLockMode(arg0);
	}

	public Metamodel getMetamodel() {
		return this.delegate.getMetamodel();
	}

	public Map<String, Object> getProperties() {
		return this.delegate.getProperties();
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		return this.delegate.getReference(arg0, arg1);
	}

	public EntityTransaction getTransaction() {
		return this.delegate.getTransaction();
	}

	public boolean isOpen() {
		return this.delegate.isOpen();
	}

	public void joinTransaction() {
		this.delegate.joinTransaction();
	}

	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		this.delegate.lock(arg0, arg1, arg2);
	}

	public void lock(Object arg0, LockModeType arg1) {
		this.delegate.lock(arg0, arg1);
	}

	public <T> T merge(T arg0) {
		return this.delegate.merge(arg0);
	}

	public void persist(Object arg0) {
		this.delegate.persist(arg0);
	}

	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		this.delegate.refresh(arg0, arg1, arg2);
	}

	public void refresh(Object arg0, LockModeType arg1) {
		this.delegate.refresh(arg0, arg1);
	}

	public void refresh(Object arg0, Map<String, Object> arg1) {
		this.delegate.refresh(arg0, arg1);
	}

	public void refresh(Object arg0) {
		this.delegate.refresh(arg0);
	}

	public void remove(Object arg0) {
		this.delegate.remove(arg0);
	}

	public void setFlushMode(FlushModeType arg0) {
		this.delegate.setFlushMode(arg0);
	}

	public void setProperty(String arg0, Object arg1) {
		this.delegate.setProperty(arg0, arg1);
	}

	public <T> T unwrap(Class<T> arg0) {
		return this.delegate.unwrap(arg0);
	}

	public MemCacheEntityManager(EntityManager delegate) {
		this.delegate = delegate;
	}
}
