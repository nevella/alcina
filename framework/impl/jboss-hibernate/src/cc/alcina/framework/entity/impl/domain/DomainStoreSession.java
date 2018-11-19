package cc.alcina.framework.entity.impl.domain;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;

@SuppressWarnings("deprecation")
public class DomainStoreSession implements Session {
	Session delegate;

	public DomainStoreSession(Session delegate) {
		this.delegate = delegate;
	}

	public Transaction beginTransaction() {
		return this.delegate.beginTransaction();
	}

	public LockRequest buildLockRequest(LockOptions arg0) {
		return this.delegate.buildLockRequest(arg0);
	}

	public void cancelQuery() throws HibernateException {
		this.delegate.cancelQuery();
	}

	public void clear() {
		this.delegate.clear();
	}

	public Connection close() throws HibernateException {
		return this.delegate.close();
	}

	public boolean contains(Object arg0) {
		return this.delegate.contains(arg0);
	}

	public Criteria createCriteria(Class arg0) {
		return this.delegate.createCriteria(arg0);
	}

	public Criteria createCriteria(Class arg0, String arg1) {
		Criteria subCriteria = this.delegate == null ? null
				: this.delegate.createCriteria(arg0, arg1);
		return new DomainStoreCriteria(arg0, arg1, subCriteria, this);
	}

	public Criteria createCriteria(String arg0) {
		return this.delegate.createCriteria(arg0);
	}

	public Criteria createCriteria(String arg0, String arg1) {
		return this.delegate.createCriteria(arg0, arg1);
	}

	public Query createFilter(Object arg0, String arg1) {
		return this.delegate.createFilter(arg0, arg1);
	}

	public Query createQuery(String arg0) {
		return this.delegate.createQuery(arg0);
	}

	public SQLQuery createSQLQuery(String arg0) {
		return this.delegate.createSQLQuery(arg0);
	}

	public void delete(Object arg0) throws HibernateException {
		this.delegate.delete(arg0);
	}

	public void delete(String arg0, Object arg1) throws HibernateException {
		this.delegate.delete(arg0, arg1);
	}

	public void disableFetchProfile(String arg0)
			throws UnknownProfileException {
		this.delegate.disableFetchProfile(arg0);
	}

	public void disableFilter(String arg0) {
		this.delegate.disableFilter(arg0);
	}

	public Connection disconnect() throws HibernateException {
		return this.delegate.disconnect();
	}

	public <T> T doReturningWork(ReturningWork<T> arg0)
			throws HibernateException {
		return this.delegate.doReturningWork(arg0);
	}

	public void doWork(Work arg0) throws HibernateException {
		this.delegate.doWork(arg0);
	}

	public void enableFetchProfile(String arg0) throws UnknownProfileException {
		this.delegate.enableFetchProfile(arg0);
	}

	public Filter enableFilter(String arg0) {
		return this.delegate.enableFilter(arg0);
	}

	public void evict(Object arg0) throws HibernateException {
		this.delegate.evict(arg0);
	}

	public void flush() throws HibernateException {
		this.delegate.flush();
	}

	public Object get(Class arg0, Serializable arg1) throws HibernateException {
		return this.delegate.get(arg0, arg1);
	}

	public Object get(Class arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	public Object get(Class arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	public Object get(String arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.get(arg0, arg1);
	}

	public Object get(String arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	public Object get(String arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	public CacheMode getCacheMode() {
		return this.delegate.getCacheMode();
	}

	public LockMode getCurrentLockMode(Object arg0) throws HibernateException {
		return this.delegate.getCurrentLockMode(arg0);
	}

	public Filter getEnabledFilter(String arg0) {
		return this.delegate.getEnabledFilter(arg0);
	}

	public EntityMode getEntityMode() {
		return this.delegate.getEntityMode();
	}

	public String getEntityName(Object arg0) throws HibernateException {
		return this.delegate.getEntityName(arg0);
	}

	public FlushMode getFlushMode() {
		return this.delegate.getFlushMode();
	}

	public Serializable getIdentifier(Object arg0) throws HibernateException {
		return this.delegate.getIdentifier(arg0);
	}

	public LobHelper getLobHelper() {
		return this.delegate.getLobHelper();
	}

	public Query getNamedQuery(String arg0) {
		return this.delegate.getNamedQuery(arg0);
	}

	public Session getSession(EntityMode arg0) {
		return this.delegate.getSession(arg0);
	}

	public SessionFactory getSessionFactory() {
		return this.delegate.getSessionFactory();
	}

	public SessionStatistics getStatistics() {
		return this.delegate.getStatistics();
	}

	public String getTenantIdentifier() {
		return this.delegate.getTenantIdentifier();
	}

	public Transaction getTransaction() {
		return this.delegate.getTransaction();
	}

	public TypeHelper getTypeHelper() {
		return this.delegate.getTypeHelper();
	}

	public boolean isConnected() {
		return this.delegate.isConnected();
	}

	public boolean isDefaultReadOnly() {
		return this.delegate.isDefaultReadOnly();
	}

	public boolean isDirty() throws HibernateException {
		return this.delegate.isDirty();
	}

	public boolean isFetchProfileEnabled(String arg0)
			throws UnknownProfileException {
		return this.delegate.isFetchProfileEnabled(arg0);
	}

	public boolean isOpen() {
		return this.delegate.isOpen();
	}

	public boolean isReadOnly(Object arg0) {
		return this.delegate.isReadOnly(arg0);
	}

	public Object load(Class arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.load(arg0, arg1);
	}

	public Object load(Class arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	public Object load(Class arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	public void load(Object arg0, Serializable arg1) throws HibernateException {
		this.delegate.load(arg0, arg1);
	}

	public Object load(String arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.load(arg0, arg1);
	}

	public Object load(String arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	public Object load(String arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	public void lock(Object arg0, LockMode arg1) throws HibernateException {
		this.delegate.lock(arg0, arg1);
	}

	public void lock(String arg0, Object arg1, LockMode arg2)
			throws HibernateException {
		this.delegate.lock(arg0, arg1, arg2);
	}

	public Object merge(Object arg0) throws HibernateException {
		return this.delegate.merge(arg0);
	}

	public Object merge(String arg0, Object arg1) throws HibernateException {
		return this.delegate.merge(arg0, arg1);
	}

	public void persist(Object arg0) throws HibernateException {
		this.delegate.persist(arg0);
	}

	public void persist(String arg0, Object arg1) throws HibernateException {
		this.delegate.persist(arg0, arg1);
	}

	public void reconnect(Connection arg0) throws HibernateException {
		this.delegate.reconnect(arg0);
	}

	public void refresh(Object arg0) throws HibernateException {
		this.delegate.refresh(arg0);
	}

	public void refresh(Object arg0, LockMode arg1) throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	public void refresh(Object arg0, LockOptions arg1)
			throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	public void refresh(String arg0, Object arg1) throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	public void refresh(String arg0, Object arg1, LockOptions arg2)
			throws HibernateException {
		this.delegate.refresh(arg0, arg1, arg2);
	}

	public void replicate(Object arg0, ReplicationMode arg1)
			throws HibernateException {
		this.delegate.replicate(arg0, arg1);
	}

	public void replicate(String arg0, Object arg1, ReplicationMode arg2)
			throws HibernateException {
		this.delegate.replicate(arg0, arg1, arg2);
	}

	public Serializable save(Object arg0) throws HibernateException {
		return this.delegate.save(arg0);
	}

	public Serializable save(String arg0, Object arg1)
			throws HibernateException {
		return this.delegate.save(arg0, arg1);
	}

	public void saveOrUpdate(Object arg0) throws HibernateException {
		this.delegate.saveOrUpdate(arg0);
	}

	public void saveOrUpdate(String arg0, Object arg1)
			throws HibernateException {
		this.delegate.saveOrUpdate(arg0, arg1);
	}

	public SharedSessionBuilder sessionWithOptions() {
		return this.delegate.sessionWithOptions();
	}

	public void setCacheMode(CacheMode arg0) {
		this.delegate.setCacheMode(arg0);
	}

	public void setDefaultReadOnly(boolean arg0) {
		this.delegate.setDefaultReadOnly(arg0);
	}

	public void setFlushMode(FlushMode arg0) {
		this.delegate.setFlushMode(arg0);
	}

	public void setReadOnly(Object arg0, boolean arg1) {
		this.delegate.setReadOnly(arg0, arg1);
	}

	public void update(Object arg0) throws HibernateException {
		this.delegate.update(arg0);
	}

	public void update(String arg0, Object arg1) throws HibernateException {
		this.delegate.update(arg0, arg1);
	}
}
