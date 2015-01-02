package cc.alcina.framework.entity.impl.cache;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.InExpression;
import org.hibernate.criterion.SimpleExpression;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CompositeFilter;
import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCacheQuery;
import cc.alcina.framework.entity.entityaccess.cache.CacheFilter;
import cc.alcina.framework.entity.entityaccess.cache.MemCacheRunner;

public class MemCacheQueryTranslator {
	AlcinaMemCacheQuery query;

	List results = new ArrayList();

	private MemCacheCriteria root;

	public List list(MemCacheCriteria criteria) throws NotHandledException {
		this.root = criteria;
		query = new AlcinaMemCacheQuery();
		addRestrictions(criteria);
		query.raw();
		new MemCacheRunner() {
			@Override
			protected void run() throws Exception {
				checkHandlesClass(root.clazz);
				List list = query.list(root.clazz);
				handleProjection(list);
			}
		};
		return results;
	}

	private void checkHandlesClass(Class clazz) throws NotHandledException {
		if (!AlcinaMemCache.get().isCached(clazz)) {
			throw new NotHandledException("Not handled class - "
					+ clazz.getSimpleName());
		}
	}

	protected void handleProjection(List hilis) {
	}

	Map<String, MemCacheCriteria> aliasLookup = new LinkedHashMap<String, MemCacheCriteria>();

	private void addRestrictions(MemCacheCriteria criteria)
			throws NotHandledException {
		aliasLookup.put(criteria.alias, criteria);
		for (MemCacheCriteria sub : criteria.subs) {
			addJoinFilter(criteria, sub);
		}
		addFilters(criteria);
		for (MemCacheCriteria sub : criteria.subs) {
			addRestrictions(sub);
		}
		addOrders(criteria);
	}

	private void addOrders(MemCacheCriteria criteria) {
		// TODO Auto-generated method stub
	}

	private void addJoinFilter(MemCacheCriteria criteria, MemCacheCriteria sub) {
		System.out.format("Ignoring join: %s %s %s\n", criteria.alias,
				sub.joinType, sub.alias);
	}

	@RegistryLocation(registryPoint = CriterionTranslator.class)
	public abstract static class CriterionTranslator<C extends Criterion> {
		public boolean handles(Class<C> clazz) {
			return clazz == getHandledClass();
		}

		protected abstract Class<C> getHandledClass();

		protected abstract CacheFilter handle(C criterion,
				MemCacheCriteria memCacheCriteria,
				MemCacheQueryTranslator translator) throws NotHandledException;

		public String getStringFieldValue(Criterion criterion, String fieldName) {
			try {
				Field pnField = criterion.getClass()
						.getDeclaredField(fieldName);
				pnField.setAccessible(true);
				return (String) pnField.get(criterion);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		protected Object getValue(Criterion criterion, String fieldName) {
			try {
				Field field = null;
				Class clazz = criterion.getClass();
				while (clazz != Object.class) {
					try {
						field = clazz.getDeclaredField(fieldName);
						break;
					} catch (Exception e) {
						clazz = clazz.getSuperclass();
					}
				}
				field.setAccessible(true);
				return field.get(criterion);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static class InExpressionTranslator extends
			CriterionTranslator<InExpression> {
		@Override
		protected Class<InExpression> getHandledClass() {
			return InExpression.class;
		}

		@Override
		protected CacheFilter handle(InExpression criterion,
				MemCacheCriteria memCacheCriteria,
				MemCacheQueryTranslator translator) {
			Object value = getValue(criterion, "values");
			Collection collection = null;
			if (value.getClass().isArray()) {
				int arrlength = Array.getLength(value);
				collection = new LinkedHashSet();
				for (int i = 0; i < arrlength; ++i) {
					collection.add(Array.get(value, i));
				}
			} else {
				collection = new LinkedHashSet((Collection) value);
			}
			return new CacheFilter(translator.translatePropertyPath(criterion,
					memCacheCriteria,
					getStringFieldValue(criterion, "propertyName")),
					collection, FilterOperator.IN);
		}
	}

	public static class SimpleExpressionTranslator extends
			CriterionTranslator<SimpleExpression> {
		@Override
		protected Class<SimpleExpression> getHandledClass() {
			return SimpleExpression.class;
		}

		@Override
		protected CacheFilter handle(SimpleExpression criterion,
				MemCacheCriteria memCacheCriteria,
				MemCacheQueryTranslator translator) throws NotHandledException {
			String propertyName = translator.translatePropertyPath(criterion,
					memCacheCriteria,
					getStringFieldValue(criterion, "propertyName"));
			String op = getStringFieldValue(criterion, "op");
			String value = getStringFieldValue(criterion, "op");
			FilterOperator fop = null;
			if (op.equals("=")) {
				fop = FilterOperator.EQ;
			} else if (op.equals("<")) {
				fop = FilterOperator.LT;
			} else if (op.equals(">")) {
				fop = FilterOperator.GT;
			} else if (op.equals("<=")) {
				fop = FilterOperator.LT_EQ;
			} else if (op.equals(">=")) {
				fop = FilterOperator.GT_EQ;
			} else if (op.equals("!=")) {
				fop = FilterOperator.NE;
			} else if (op.equals("<>")) {
				fop = FilterOperator.NE;
			}
			if (fop == null) {
				throw new NotHandledException("Not handled operator - " + op);
			}
			return new CacheFilter(propertyName, getValue(criterion, "value"),
					fop);
		}
	}

	public static class ConjunctionTranslator extends
			CriterionTranslator<Conjunction> {
		@Override
		protected Class<Conjunction> getHandledClass() {
			return Conjunction.class;
		}

		@Override
		protected CacheFilter handle(Conjunction criterion,
				MemCacheCriteria memCacheCriteria,
				MemCacheQueryTranslator translator) throws NotHandledException {
			List<Criterion> subs = (List<Criterion>) getValue(criterion,
					"criteria");
			if (subs.size() == 1) {
				return translator.criterionToFilter(memCacheCriteria,
						subs.get(0));
			}
			CompositeFilter filter = new CompositeFilter();
			for (Criterion sub : subs) {
				filter.add(translator.criterionToFilter(memCacheCriteria, sub)
						.asCollectionFilter());
			}
			return new CacheFilter(filter);
		}
	}

	public static class DisjunctionTranslator extends
			CriterionTranslator<Disjunction> {
		@Override
		protected Class<Disjunction> getHandledClass() {
			return Disjunction.class;
		}

		@Override
		protected CacheFilter handle(Disjunction criterion,
				MemCacheCriteria memCacheCriteria,
				MemCacheQueryTranslator translator) throws NotHandledException {
			List<Criterion> subs = (List<Criterion>) getValue(criterion,
					"criteria");
			if (subs.size() == 1) {
				return translator.criterionToFilter(memCacheCriteria,
						subs.get(0));
			}
			CompositeFilter filter = new CompositeFilter(true);
			for (Criterion sub : subs) {
				filter.add(translator.criterionToFilter(memCacheCriteria, sub)
						.asCollectionFilter());
			}
			return new CacheFilter(filter);
		}
	}

	private void addFilters(MemCacheCriteria criteria)
			throws NotHandledException {
		for (Criterion criterion : criteria.criterions) {
			query.filter(criterionToFilter(criteria, criterion));
		}
	}

	public String translatePropertyPath(Criterion criterion,
			MemCacheCriteria context, String propertyPath) {
		if (propertyPath.contains(".")) {
			int idx = propertyPath.indexOf(".");
			String prefix = propertyPath.substring(0, idx);
			if (prefix.equals(root.alias)) {
				propertyPath = propertyPath.substring(idx + 1);
			} else {
				int debug = 3;
				MemCacheCriteria sub = aliasLookup.get(prefix);
				propertyPath = sub.associationPath + "."
						+ propertyPath.substring(idx + 1);
			}
		}
		return propertyPath;
	}

	protected CacheFilter criterionToFilter(MemCacheCriteria memCacheCriteria,
			Criterion criterion) throws NotHandledException {
		boolean handled = false;
		for (CriterionTranslator translator : Registry
				.impls(CriterionTranslator.class)) {
			if (translator.handles(criterion.getClass())) {
				return translator.handle(criterion, memCacheCriteria, this);
			}
		}
		throw new NotHandledException(criterion);
	}
}
