package cc.alcina.framework.entity.impl.domain;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.criterion.AliasedProjection;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.CountProjection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.InExpression;
import org.hibernate.criterion.NotExpression;
import org.hibernate.criterion.NullExpression;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.transform.ResultTransformer;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.domain.CompositeFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.entityaccess.cache.DomainRunner;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreQuery;
import cc.alcina.framework.entity.entityaccess.cache.NotCacheFilter;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.CriterionTranslator;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.FieldHelper;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.GroupedRow;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.GroupedRows;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.GroupedValue;
import cc.alcina.framework.entity.impl.domain.DomainStoreQueryTranslator.ProjectionHelper;

public class DomainStoreQueryTranslator {
	DomainStoreQuery query;

	private DomainStoreCriteria root;

	private List rawRows;

	private GroupedRows groupedRows;

	Map<String, DomainStoreCriteria> aliasLookup = new LinkedHashMap<String, DomainStoreCriteria>();

	List<ProjectionHelper> projectionHelpers = new ArrayList<DomainStoreQueryTranslator.ProjectionHelper>();

	private boolean aggregateQuery;

	public List list(DomainStoreCriteria criteria) throws NotHandledException {
		this.root = criteria;
		query = DomainStore.stores().query(root.clazz);
		addRestrictions(criteria);
		query.raw();
		new DomainRunner() {
			@Override
			protected void run() throws Exception {
				checkHandlesClass(root.clazz);
				rawRows = query.list();
				handleProjections();
			}
		};
		ResultTransformer resultTransformer = criteria.getResultTransformer();
		List results = groupedRows.asTuples();
		if (resultTransformer != null) {
			Stream stream = results.stream().map(tp -> resultTransformer
					.transformTuple((Object[]) tp, null));
			results = (List) stream.collect(Collectors.toList());
		}
		return results;
	}

	public String translatePropertyPath(Criterion criterion,
			DomainStoreCriteria context, String propertyPath) {
		if (propertyPath.contains(".")) {
			int idx = propertyPath.indexOf(".");
			String prefix = propertyPath.substring(0, idx);
			if (prefix.equals(root.alias)) {
				propertyPath = propertyPath.substring(idx + 1);
			} else {
				DomainStoreCriteria sub = aliasLookup.get(prefix);
				if (sub == null) {
					// no alias to root, just use original path
				} else {
					propertyPath = sub.associationPath + "."
							+ propertyPath.substring(idx + 1);
				}
			}
		}
		propertyPath = query.getCanonicalPropertyPath(root.clazz, propertyPath);
		return propertyPath;
	}

	private void addFilters(DomainStoreCriteria criteria)
			throws NotHandledException {
		for (Criterion criterion : criteria.criterions) {
			query.filter(criterionToFilter(criteria, criterion));
		}
	}

	private void addJoinFilter(DomainStoreCriteria criteria,
			DomainStoreCriteria sub) {
		switch (sub.joinType) {
		case INNER_JOIN:
		case LEFT_OUTER_JOIN:
			// implicitly handled, since we're linking from root. note that
			// INNER_JOIN might confuse some count() filters - but rely on 'if
			// it's an inner join, guaranteed non-null' .... for the mo
			return;
		}
		System.out.format("Ignoring join: %s %s %s\n", criteria.alias,
				sub.joinType, sub.alias);
	}

	private void addOrders(DomainStoreCriteria criteria) {
		// TODO Auto-generated method stub
	}

	private void addRestrictions(DomainStoreCriteria criteria)
			throws NotHandledException {
		aliasLookup.put(criteria.alias, criteria);
		for (DomainStoreCriteria sub : criteria.subs) {
			addJoinFilter(criteria, sub);
		}
		addFilters(criteria);
		for (DomainStoreCriteria sub : criteria.subs) {
			addRestrictions(sub);
		}
		addOrders(criteria);
	}

	private void checkHandlesClass(Class clazz) throws NotHandledException {
		if (!query.getStore().isCached(clazz)) {
			throw new NotHandledException(
					"Not handled class - " + clazz.getSimpleName());
		}
	}

	private void setupProjectionHelpers() throws Exception {
		List<Projection> projections = new ArrayList<>();
		if (root.projection instanceof ProjectionList) {
			ProjectionList projectionList = (ProjectionList) root.projection;
			Field field = ProjectionList.class.getDeclaredField("elements");
			field.setAccessible(true);
			projections = (List) field.get(projectionList);
		} else {
			projections.add(root.projection);
		}
		int groupCount = 0;
		aggregateQuery = true;
		for (Projection projection : projections) {
			ProjectionHelper helper = new ProjectionHelper(projection);
			projectionHelpers.add(helper);
			aggregateQuery &= helper.isAggregate();
			groupCount += helper.isGrouped() ? 1 : 0;
		}
		groupedRows = new GroupedRows(groupCount);
	}

	protected DomainFilter criterionToFilter(
			DomainStoreCriteria domainStoreCriteria, Criterion criterion)
			throws NotHandledException {
		boolean handled = false;
		for (CriterionTranslator translator : Registry
				.impls(CriterionTranslator.class)) {
			if (translator.handles(criterion.getClass())) {
				return translator.handle(criterion, domainStoreCriteria, this);
			}
		}
		throw new NotHandledException(criterion);
	}

	protected void handleProjections() throws Exception {
		setupProjectionHelpers();
		if (rawRows.size() > 0 || !aggregateQuery) {
			for (Object obj : rawRows) {
				int i = 0;
				for (ProjectionHelper projectionHelper : projectionHelpers) {
					groupedRows.handleProjection(obj, projectionHelper, i++);
				}
			}
		} else {
			int i = 0;
			for (ProjectionHelper projectionHelper : projectionHelpers) {
				groupedRows.handleProjection(null, projectionHelper, i++);
			}
		}
	}

	public static class ConjunctionTranslator
			extends CriterionTranslator<Conjunction> {
		@Override
		protected Class<Conjunction> getHandledClass() {
			return Conjunction.class;
		}

		@Override
		protected DomainFilter handle(Conjunction criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException {
			List<Criterion> subs = (List<Criterion>) getValue(criterion,
					"criteria", "conditions");
			if (subs.size() == 1) {
				return translator.criterionToFilter(domainStoreCriteria,
						subs.get(0));
			}
			CompositeFilter filter = new CompositeFilter();
			for (Criterion sub : subs) {
				filter.add(
						translator.criterionToFilter(domainStoreCriteria, sub));
			}
			return filter;
		}
	}

	@RegistryLocation(registryPoint = CriterionTranslator.class)
	public abstract static class CriterionTranslator<C extends Criterion> {
		FieldHelper fieldHelper = new FieldHelper();

		public String getStringFieldValue(Criterion criterion,
				String fieldName) {
			return fieldHelper.getValue(criterion, fieldName);
		}

		public boolean handles(Class<C> clazz) {
			return clazz == getHandledClass();
		}

		protected abstract Class<C> getHandledClass();

		protected Object getValue(Criterion criterion, String... fieldNames) {
			return fieldHelper.getValue(criterion, fieldNames);
		}

		protected abstract DomainFilter handle(C criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException;
	}

	public static class DisjunctionTranslator
			extends CriterionTranslator<Disjunction> {
		@Override
		protected Class<Disjunction> getHandledClass() {
			return Disjunction.class;
		}

		@Override
		protected DomainFilter handle(Disjunction criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException {
			List<Criterion> subs = (List<Criterion>) getValue(criterion,
					"criteria", "conditions");
			if (subs.size() == 1) {
				return translator.criterionToFilter(domainStoreCriteria,
						subs.get(0));
			}
			CompositeFilter filter = new CompositeFilter(true);
			for (Criterion sub : subs) {
				filter.add(
						translator.criterionToFilter(domainStoreCriteria, sub));
			}
			return filter;
		}
	}

	public static class InExpressionTranslator
			extends CriterionTranslator<InExpression> {
		@Override
		protected Class<InExpression> getHandledClass() {
			return InExpression.class;
		}

		@Override
		protected DomainFilter handle(InExpression criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator) {
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
			return new DomainFilter(
					translator.translatePropertyPath(criterion,
							domainStoreCriteria,
							getStringFieldValue(criterion, "propertyName")),
					collection, FilterOperator.IN);
		}
	}

	public static class NotTranslator
			extends CriterionTranslator<NotExpression> {
		@Override
		protected Class<NotExpression> getHandledClass() {
			return NotExpression.class;
		}

		@Override
		protected DomainFilter handle(NotExpression criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException {
			Criterion sub = (Criterion) getValue(criterion, "criterion");
			NotCacheFilter filter = new NotCacheFilter(
					translator.criterionToFilter(domainStoreCriteria, sub));
			return filter;
		}
	}

	public static class NullTranslator
			extends CriterionTranslator<NullExpression> {
		@Override
		protected Class<NullExpression> getHandledClass() {
			return NullExpression.class;
		}

		@Override
		protected DomainFilter handle(NullExpression criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException {
			String propertyName = translator.translatePropertyPath(criterion,
					domainStoreCriteria,
					getStringFieldValue(criterion, "propertyName"));
			return new DomainFilter(propertyName, null, FilterOperator.EQ);
		}
	}

	public static class SimpleExpressionTranslator
			extends CriterionTranslator<SimpleExpression> {
		@Override
		protected Class<SimpleExpression> getHandledClass() {
			return SimpleExpression.class;
		}

		@Override
		protected DomainFilter handle(SimpleExpression criterion,
				DomainStoreCriteria domainStoreCriteria,
				DomainStoreQueryTranslator translator)
				throws NotHandledException {
			String propertyName = translator.translatePropertyPath(criterion,
					domainStoreCriteria,
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
			return new DomainFilter(propertyName, getValue(criterion, "value"),
					fop);
		}
	}

	static class FieldHelper {
		Field field = null;

		Object lastFrom = null;

		String[] lastFieldNames = null;

		protected <T> T getValue(Object from, String... fieldNames) {
			try {
				if (field != null) {
					if (from != lastFrom
							|| !Arrays.equals(lastFieldNames, fieldNames)) {
						field = null;
					}
				}
				if (field == null) {
					Class clazz = from.getClass();
					while (clazz != Object.class) {
						for (String fieldName : fieldNames) {
							try {
								field = clazz.getDeclaredField(fieldName);
								break;
							} catch (Exception e) {
							}
						}
						if (field != null) {
							break;
						} else {
							clazz = clazz.getSuperclass();
						}
					}
					field.setAccessible(true);
					lastFrom = from;
					lastFieldNames = fieldNames;
				}
				return (T) field.get(from);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class GroupedRow {
		List<GroupedValue> groupedValues;

		public int handledCount;

		public boolean added;

		Multiset<Integer, Set> seen = new Multiset<Integer, Set>();

		public GroupedRow() {
			groupedValues = new ArrayList<GroupedValue>(
					projectionHelpers.size());
			for (int i = 0; i < projectionHelpers.size(); i++) {
				groupedValues.add(new GroupedValue(projectionHelpers.get(i)));
			}
		}

		public Object asTuple() {
			Object[] tuple = new Object[groupedValues.size()];
			if (groupedValues.size() == 1) {
				return groupedValues.get(0).getValue();
			}
			for (int i = 0; i < tuple.length; i++) {
				tuple[i] = groupedValues.get(i).getValue();
			}
			return tuple;
		}

		public Object[] groupKeys() {
			Object[] keys = new Object[groupedRows.groupCount];
			for (int i = 0; i < groupedRows.groupCount; i++) {
				keys[i] = groupedValues.get(i).getValue();
			}
			return keys;
		}

		public void handleProjection(Object obj,
				ProjectionHelper projectionHelper, int index) {
			if (projectionHelper.isCount()) {
				if (obj != null) {
					boolean ignore = false;
					if (projectionHelper.isCountDistinct()) {
						Object value = projectionHelper.getValue(obj);
						ignore = !seen.add(index, value);
					}
					if (!ignore) {
						groupedValues.get(index).incrementCount();
					}
				}
			} else {
				groupedValues.get(index).value = projectionHelper.getValue(obj);
				handledCount++;
			}
		}

		@Override
		public String toString() {
			return groupedValues.toString();
		}
	}

	class GroupedRows {
		List<GroupedRow> rows = new ArrayList<GroupedRow>();

		GroupedRow currentRow;

		UnsortedMultikeyMap<GroupedRow> existingRows;

		private int groupCount;

		public GroupedRows(int groupCount) {
			this.groupCount = groupCount;
			if (groupCount == 0) {
			} else {
				existingRows = new UnsortedMultikeyMap<DomainStoreQueryTranslator.GroupedRow>(
						groupCount);
			}
		}

		public List asTuples() {
			return CollectionFilters.convert(rows,
					new Converter<GroupedRow, Object>() {
						@Override
						public Object convert(GroupedRow original) {
							return original.asTuple();
						}
					});
		}

		public void handleProjection(Object obj,
				ProjectionHelper projectionHelper, int i) {
			if (currentRow == null) {
				currentRow = new GroupedRow();
			}
			currentRow.handleProjection(obj, projectionHelper, i);
			// we've set all the keys for this row, put in lookup (or get)
			if (groupCount != 0 && currentRow.handledCount == groupCount) {
				Object[] groupKeys = currentRow.groupKeys();
				GroupedRow existing = existingRows.get(groupKeys);
				if (existing != null) {
					currentRow = existing;
				} else {
					Object[] keysAndValue = new Object[groupKeys.length + 1];
					System.arraycopy(groupKeys, 0, keysAndValue, 0,
							groupKeys.length);
					keysAndValue[groupKeys.length] = currentRow;
					existingRows.put(keysAndValue);
				}
			}
			if (i == projectionHelpers.size() - 1) {
				if (currentRow.added) {
				} else {
					rows.add(currentRow);
					currentRow.added = true;
				}
				currentRow = null;
			}
		}

		@Override
		public String toString() {
			return CommonUtils.join(CollectionFilters.convert(projectionHelpers,
					new Converter<ProjectionHelper, String>() {
						@Override
						public String convert(ProjectionHelper original) {
							return original.toString();
						}
					}), "\n") + "==============\n"
					+ CommonUtils.join(CollectionFilters.convert(rows,
							new Converter<GroupedRow, String>() {
								@Override
								public String convert(GroupedRow original) {
									return original.toString();
								}
							}), "\n");
		}
	}

	static class GroupedValue {
		long count;

		Object value;

		private ProjectionHelper projectionHelper;

		public GroupedValue(ProjectionHelper projectionHelper) {
			this.projectionHelper = projectionHelper;
		}

		public void incrementCount() {
			count++;
		}

		@Override
		public String toString() {
			return CommonUtils.nullSafeToString(getValue());
		}

		Object getValue() {
			if (projectionHelper.isCount()) {
				return count;
			} else {
				return value;
			}
		}
	}

	class ProjectionHelper {
		boolean count;

		boolean distinct;

		FieldHelper fieldHelper = new FieldHelper();

		PropertyPathAccessor accessor = null;

		Projection projection;

		public ProjectionHelper(Projection projection) {
			try {
				if (projection instanceof AliasedProjection) {
					projection = fieldHelper.getValue(projection, "projection");
				}
				this.projection = projection;
				if (projection instanceof CountProjection) {
					count = true;
					distinct = fieldHelper.getValue(projection, "distinct");
				}
				if (!isCount() || isCountDistinct()) {
					setupAccessor();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public Object getValue(Object obj) {
			return accessor.getChainedProperty(obj);
		}

		public boolean isAggregate() {
			return isCount();
		}

		public boolean isCount() {
			return count;
		}

		public boolean isCountDistinct() {
			return count && distinct;
		}

		@Override
		public String toString() {
			return projection.toString();
		}

		private void setupAccessor() throws Exception {
			if (projection instanceof PropertyProjection
					|| projection instanceof CountProjection) {
				String propertyPath = fieldHelper.getValue(projection,
						"propertyName");
				propertyPath = translatePropertyPath(null, null, propertyPath);
				accessor = new PropertyPathAccessor(propertyPath);
			} else {
				throw new NotHandledException(projection.toString());
			}
		}

		boolean isGrouped() {
			return projection.isGrouped();
		}
	}
}
