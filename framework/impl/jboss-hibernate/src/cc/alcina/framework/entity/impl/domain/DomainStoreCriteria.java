package cc.alcina.framework.entity.impl.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

@SuppressWarnings("deprecation")
public class DomainStoreCriteria implements Criteria {
	Class clazz;

	String alias;

	private Criteria entityManagerCriteria;

	private DomainStoreSession domainStoreSession;

	JoinType joinType;

	String associationPath;

	List<DomainStoreCriteria> subs = new ArrayList<DomainStoreCriteria>();

	List<Criterion> criterions = new ArrayList<Criterion>();

	List<Order> orders = new ArrayList<Order>();

	Projection projection;

	int maxResults;

	int firstResult;

	private ResultTransformer resultTransformer;

	public DomainStoreCriteria(Class clazz, String alias,
			Criteria entityManagerCriteria,
			DomainStoreSession domainStoreSession) {
		this(clazz, alias, entityManagerCriteria, JoinType.NONE,
				domainStoreSession);
	}

	public DomainStoreCriteria(Class clazz, String alias,
			Criteria entityManagerCriteria, JoinType joinType,
			DomainStoreSession domainStoreSession) {
		this(clazz, alias, null, entityManagerCriteria, joinType,
				domainStoreSession);
	}

	public DomainStoreCriteria(Class clazz, String alias,
			String associationPath, Criteria entityManagerCriteria,
			JoinType joinType, DomainStoreSession domainStoreSession) {
		this.clazz = clazz;
		this.alias = alias;
		this.associationPath = associationPath;
		this.entityManagerCriteria = entityManagerCriteria;
		this.joinType = joinType;
		this.domainStoreSession = domainStoreSession;
	}

	@Override
	public Criteria add(Criterion arg0) {
		if (this.entityManagerCriteria != null) {
			this.entityManagerCriteria.add(arg0);
		}
		criterions.add(arg0);
		return this;
	}

	@Override
	public Criteria addOrder(Order order) {
		if (this.entityManagerCriteria != null) {
			this.entityManagerCriteria.addOrder(order);
		}
		this.orders.add(order);
		return this;
	}

	@Override
	public Criteria addQueryHint(String hint) {
		return this;
	}

	@Override
	public Criteria createAlias(String arg0, String arg1)
			throws HibernateException {
		return this.entityManagerCriteria.createAlias(arg0, arg1);
	}

	@Override
	public Criteria createAlias(String arg0, String arg1, int arg2)
			throws HibernateException {
		return this.entityManagerCriteria.createAlias(arg0, arg1, arg2);
	}

	@Override
	public Criteria createAlias(String arg0, String arg1, int arg2,
			Criterion arg3) throws HibernateException {
		return this.entityManagerCriteria.createAlias(arg0, arg1, arg2, arg3);
	}

	@Override
	public Criteria createAlias(String arg0, String arg1, JoinType arg2)
			throws HibernateException {
		return this.entityManagerCriteria.createAlias(arg0, arg1, arg2);
	}

	@Override
	public Criteria createAlias(String arg0, String arg1, JoinType arg2,
			Criterion arg3) throws HibernateException {
		return this.entityManagerCriteria.createAlias(arg0, arg1, arg2, arg3);
	}

	@Override
	public Criteria createCriteria(String arg0) throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0);
	}

	@Override
	public Criteria createCriteria(String arg0, int arg1)
			throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1);
	}

	@Override
	public Criteria createCriteria(String arg0, JoinType arg1)
			throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1);
	}

	@Override
	public Criteria createCriteria(String arg0, String arg1)
			throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1);
	}

	@Override
	public Criteria createCriteria(String arg0, String arg1, int arg2)
			throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1, arg2);
	}

	@Override
	public Criteria createCriteria(String arg0, String arg1, int arg2,
			Criterion arg3) throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1, arg2,
				arg3);
	}

	@Override
	public Criteria createCriteria(String associationPath, String alias,
			JoinType arg2) throws HibernateException {
		Criteria subCriteria = this.entityManagerCriteria == null ? null
				: this.entityManagerCriteria.createCriteria(associationPath,
						alias);
		DomainStoreCriteria newCriteria = new DomainStoreCriteria(null, alias,
				associationPath, subCriteria, arg2, domainStoreSession);
		subs.add(newCriteria);
		return newCriteria;
	}

	@Override
	public Criteria createCriteria(String arg0, String arg1, JoinType arg2,
			Criterion arg3) throws HibernateException {
		return this.entityManagerCriteria.createCriteria(arg0, arg1, arg2,
				arg3);
	}

	@Override
	public String getAlias() {
		return this.entityManagerCriteria.getAlias();
	}

	public ResultTransformer getResultTransformer() {
		return this.resultTransformer;
	}

	@Override
	public boolean isReadOnly() {
		return this.entityManagerCriteria.isReadOnly();
	}

	@Override
	public boolean isReadOnlyInitialized() {
		return this.entityManagerCriteria.isReadOnlyInitialized();
	}

	@Override
	public List list() throws HibernateException {
		try {
			return new DomainStoreQueryTranslator().list(this);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new HibernateException(ex);
			// return this.entityManagerCriteria.list();
		}
	}

	@Override
	public ScrollableResults scroll() throws HibernateException {
		return this.entityManagerCriteria.scroll();
	}

	@Override
	public ScrollableResults scroll(ScrollMode arg0) throws HibernateException {
		return this.entityManagerCriteria.scroll(arg0);
	}

	@Override
	public Criteria setCacheable(boolean arg0) {
		return this.entityManagerCriteria.setCacheable(arg0);
	}

	@Override
	public Criteria setCacheMode(CacheMode arg0) {
		return this.entityManagerCriteria.setCacheMode(arg0);
	}

	@Override
	public Criteria setCacheRegion(String arg0) {
		return this.entityManagerCriteria.setCacheRegion(arg0);
	}

	@Override
	public Criteria setComment(String arg0) {
		return this.entityManagerCriteria.setComment(arg0);
	}

	@Override
	public Criteria setFetchMode(String arg0, FetchMode arg1)
			throws HibernateException {
		return this.entityManagerCriteria.setFetchMode(arg0, arg1);
	}

	@Override
	public Criteria setFetchSize(int arg0) {
		return this.entityManagerCriteria.setFetchSize(arg0);
	}

	@Override
	public Criteria setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		if (entityManagerCriteria != null) {
			this.entityManagerCriteria.setFirstResult(firstResult);
		}
		return this;
	}

	@Override
	public Criteria setFlushMode(FlushMode arg0) {
		return this.entityManagerCriteria.setFlushMode(arg0);
	}

	@Override
	public Criteria setLockMode(LockMode arg0) {
		return this.entityManagerCriteria.setLockMode(arg0);
	}

	@Override
	public Criteria setLockMode(String arg0, LockMode arg1) {
		return this.entityManagerCriteria.setLockMode(arg0, arg1);
	}

	@Override
	public Criteria setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		if (entityManagerCriteria != null) {
			this.entityManagerCriteria.setMaxResults(maxResults);
		}
		return this;
	}

	@Override
	public Criteria setProjection(Projection projection) {
		if (entityManagerCriteria != null) {
			entityManagerCriteria.setProjection(projection);
		}
		this.projection = projection;
		return this;
	}

	@Override
	public Criteria setReadOnly(boolean arg0) {
		return this.entityManagerCriteria.setReadOnly(arg0);
	}

	@Override
	public Criteria setResultTransformer(ResultTransformer resultTransformer) {
		this.resultTransformer = resultTransformer;
		if (this.entityManagerCriteria != null) {
			return this.entityManagerCriteria
					.setResultTransformer(resultTransformer);
		} else {
			return null;
		}
	}

	@Override
	public Criteria setTimeout(int arg0) {
		return this.entityManagerCriteria.setTimeout(arg0);
	}

	@Override
	public Object uniqueResult() throws HibernateException {
		return this.entityManagerCriteria.uniqueResult();
	}
}
