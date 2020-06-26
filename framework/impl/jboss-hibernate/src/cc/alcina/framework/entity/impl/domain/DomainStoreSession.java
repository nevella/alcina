package cc.alcina.framework.entity.impl.domain;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.stat.SessionStatistics;

@SuppressWarnings("deprecation")
public class DomainStoreSession implements Session {
	Session delegate;

	public DomainStoreSession(Session delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		this.delegate.addEventListeners(listeners);
	}

	@Override
	public Transaction beginTransaction() {
		return this.delegate.beginTransaction();
	}

	@Override
	public LockRequest buildLockRequest(LockOptions arg0) {
		return this.delegate.buildLockRequest(arg0);
	}

	@Override
	public IdentifierLoadAccess byId(Class entityClass) {
		return this.delegate.byId(entityClass);
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return this.delegate.byId(entityName);
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		return this.delegate.byNaturalId(entityClass);
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return this.delegate.byNaturalId(entityName);
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		return this.delegate.bySimpleNaturalId(entityClass);
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return this.delegate.bySimpleNaturalId(entityName);
	}

	@Override
	public void cancelQuery() throws HibernateException {
		this.delegate.cancelQuery();
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public Connection close() throws HibernateException {
		return this.delegate.close();
	}

	@Override
	public boolean contains(Object arg0) {
		return this.delegate.contains(arg0);
	}

	@Override
	public Criteria createCriteria(Class arg0) {
		return this.delegate.createCriteria(arg0);
	}

	@Override
	public Criteria createCriteria(Class arg0, String arg1) {
		Criteria subCriteria = this.delegate == null ? null
				: this.delegate.createCriteria(arg0, arg1);
		return new DomainStoreCriteria(arg0, arg1, subCriteria, this);
	}

	@Override
	public Criteria createCriteria(String arg0) {
		return this.delegate.createCriteria(arg0);
	}

	@Override
	public Criteria createCriteria(String arg0, String arg1) {
		return this.delegate.createCriteria(arg0, arg1);
	}

	@Override
	public Query createFilter(Object arg0, String arg1) {
		return this.delegate.createFilter(arg0, arg1);
	}

	@Override
	public Query createQuery(String arg0) {
		return this.delegate.createQuery(arg0);
	}

	@Override
	public SQLQuery createSQLQuery(String arg0) {
		return this.delegate.createSQLQuery(arg0);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return this.delegate.createStoredProcedureCall(procedureName);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName,
			Class... resultClasses) {
		return this.delegate.createStoredProcedureCall(procedureName,
				resultClasses);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName,
			String... resultSetMappings) {
		return this.delegate.createStoredProcedureCall(procedureName,
				resultSetMappings);
	}

	@Override
	public void delete(Object arg0) throws HibernateException {
		this.delegate.delete(arg0);
	}

	@Override
	public void delete(String arg0, Object arg1) throws HibernateException {
		this.delegate.delete(arg0, arg1);
	}

	@Override
	public void disableFetchProfile(String arg0)
			throws UnknownProfileException {
		this.delegate.disableFetchProfile(arg0);
	}

	@Override
	public void disableFilter(String arg0) {
		this.delegate.disableFilter(arg0);
	}

	@Override
	public Connection disconnect() throws HibernateException {
		return this.delegate.disconnect();
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> arg0)
			throws HibernateException {
		return this.delegate.doReturningWork(arg0);
	}

	@Override
	public void doWork(Work arg0) throws HibernateException {
		this.delegate.doWork(arg0);
	}

	@Override
	public void enableFetchProfile(String arg0) throws UnknownProfileException {
		this.delegate.enableFetchProfile(arg0);
	}

	@Override
	public Filter enableFilter(String arg0) {
		return this.delegate.enableFilter(arg0);
	}

	@Override
	public void evict(Object arg0) throws HibernateException {
		this.delegate.evict(arg0);
	}

	@Override
	public void flush() throws HibernateException {
		this.delegate.flush();
	}

	@Override
	public Object get(Class arg0, Serializable arg1) throws HibernateException {
		return this.delegate.get(arg0, arg1);
	}

	@Override
	public Object get(Class arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	@Override
	public Object get(Class arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	@Override
	public Object get(String arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.get(arg0, arg1);
	}

	@Override
	public Object get(String arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	@Override
	public Object get(String arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.get(arg0, arg1, arg2);
	}

	@Override
	public CacheMode getCacheMode() {
		return this.delegate.getCacheMode();
	}

	@Override
	public LockMode getCurrentLockMode(Object arg0) throws HibernateException {
		return this.delegate.getCurrentLockMode(arg0);
	}

	@Override
	public Filter getEnabledFilter(String arg0) {
		return this.delegate.getEnabledFilter(arg0);
	}

	@Override
	public String getEntityName(Object arg0) throws HibernateException {
		return this.delegate.getEntityName(arg0);
	}

	@Override
	public FlushMode getFlushMode() {
		return this.delegate.getFlushMode();
	}

	@Override
	public Serializable getIdentifier(Object arg0) throws HibernateException {
		return this.delegate.getIdentifier(arg0);
	}

	@Override
	public LobHelper getLobHelper() {
		return this.delegate.getLobHelper();
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return this.delegate.getNamedProcedureCall(name);
	}

	@Override
	public Query getNamedQuery(String arg0) {
		return this.delegate.getNamedQuery(arg0);
	}

	@Override
	public SessionFactory getSessionFactory() {
		return this.delegate.getSessionFactory();
	}

	@Override
	public SessionStatistics getStatistics() {
		return this.delegate.getStatistics();
	}

	@Override
	public String getTenantIdentifier() {
		return this.delegate.getTenantIdentifier();
	}

	@Override
	public Transaction getTransaction() {
		return this.delegate.getTransaction();
	}

	@Override
	public TypeHelper getTypeHelper() {
		return this.delegate.getTypeHelper();
	}

	@Override
	public boolean isConnected() {
		return this.delegate.isConnected();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return this.delegate.isDefaultReadOnly();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		return this.delegate.isDirty();
	}

	@Override
	public boolean isFetchProfileEnabled(String arg0)
			throws UnknownProfileException {
		return this.delegate.isFetchProfileEnabled(arg0);
	}

	@Override
	public boolean isOpen() {
		return this.delegate.isOpen();
	}

	@Override
	public boolean isReadOnly(Object arg0) {
		return this.delegate.isReadOnly(arg0);
	}

	@Override
	public Object load(Class arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.load(arg0, arg1);
	}

	@Override
	public Object load(Class arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	@Override
	public Object load(Class arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	@Override
	public void load(Object arg0, Serializable arg1) throws HibernateException {
		this.delegate.load(arg0, arg1);
	}

	@Override
	public Object load(String arg0, Serializable arg1)
			throws HibernateException {
		return this.delegate.load(arg0, arg1);
	}

	@Override
	public Object load(String arg0, Serializable arg1, LockMode arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	@Override
	public Object load(String arg0, Serializable arg1, LockOptions arg2)
			throws HibernateException {
		return this.delegate.load(arg0, arg1, arg2);
	}

	@Override
	public void lock(Object arg0, LockMode arg1) throws HibernateException {
		this.delegate.lock(arg0, arg1);
	}

	@Override
	public void lock(String arg0, Object arg1, LockMode arg2)
			throws HibernateException {
		this.delegate.lock(arg0, arg1, arg2);
	}

	@Override
	public Object merge(Object arg0) throws HibernateException {
		return this.delegate.merge(arg0);
	}

	@Override
	public Object merge(String arg0, Object arg1) throws HibernateException {
		return this.delegate.merge(arg0, arg1);
	}

	@Override
	public void persist(Object arg0) throws HibernateException {
		this.delegate.persist(arg0);
	}

	@Override
	public void persist(String arg0, Object arg1) throws HibernateException {
		this.delegate.persist(arg0, arg1);
	}

	@Override
	public void reconnect(Connection arg0) throws HibernateException {
		this.delegate.reconnect(arg0);
	}

	@Override
	public void refresh(Object arg0) throws HibernateException {
		this.delegate.refresh(arg0);
	}

	@Override
	public void refresh(Object arg0, LockMode arg1) throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	@Override
	public void refresh(Object arg0, LockOptions arg1)
			throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	@Override
	public void refresh(String arg0, Object arg1) throws HibernateException {
		this.delegate.refresh(arg0, arg1);
	}

	@Override
	public void refresh(String arg0, Object arg1, LockOptions arg2)
			throws HibernateException {
		this.delegate.refresh(arg0, arg1, arg2);
	}

	@Override
	public void replicate(Object arg0, ReplicationMode arg1)
			throws HibernateException {
		this.delegate.replicate(arg0, arg1);
	}

	@Override
	public void replicate(String arg0, Object arg1, ReplicationMode arg2)
			throws HibernateException {
		this.delegate.replicate(arg0, arg1, arg2);
	}

	@Override
	public Serializable save(Object arg0) throws HibernateException {
		return this.delegate.save(arg0);
	}

	@Override
	public Serializable save(String arg0, Object arg1)
			throws HibernateException {
		return this.delegate.save(arg0, arg1);
	}

	@Override
	public void saveOrUpdate(Object arg0) throws HibernateException {
		this.delegate.saveOrUpdate(arg0);
	}

	@Override
	public void saveOrUpdate(String arg0, Object arg1)
			throws HibernateException {
		this.delegate.saveOrUpdate(arg0, arg1);
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return this.delegate.sessionWithOptions();
	}

	@Override
	public void setCacheMode(CacheMode arg0) {
		this.delegate.setCacheMode(arg0);
	}

	@Override
	public void setDefaultReadOnly(boolean arg0) {
		this.delegate.setDefaultReadOnly(arg0);
	}

	@Override
	public void setFlushMode(FlushMode arg0) {
		this.delegate.setFlushMode(arg0);
	}

	@Override
	public void setReadOnly(Object arg0, boolean arg1) {
		this.delegate.setReadOnly(arg0, arg1);
	}

	@Override
	public void update(Object arg0) throws HibernateException {
		this.delegate.update(arg0);
	}

	@Override
	public void update(String arg0, Object arg1) throws HibernateException {
		this.delegate.update(arg0, arg1);
	}
}
