package cc.alcina.framework.servlet.sync;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLog;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLogItem;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogStatus;
import cc.alcina.framework.servlet.sync.SyncLogger.SyncLoggerRow;
import cc.alcina.framework.servlet.sync.SyncPair.SyncPairAction;

/**
 * Target is 'right' - so if left doesn't exist, will be a delete - if right
 * doesn't, a create
 * 
 * Note that left *must* have distinct keys (if using key matcher strategy)
 * 
 * 
 * 
 * In general, when adding a merge field, make sure that the object's
 * equivalentTo checks the new field
 */
public class SyncMerger<T> {
	public static MergeFilter RIGHT_IS_DEFINITIVE = new MergeFilter() {
		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return false;
		}

		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return true;
		}
	};

	public static MergeFilter LEFT_IS_DEFINITIVE = new MergeFilter() {
		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return true;
		}

		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return false;
		}
	};

	public static final MergeFilter NO_OVERWRITE_FILTER = new MergeFilter() {
		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return rightProp == null;
		}

		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return leftProp == null;
		}
	};

	protected StringKeyProvider<T> keyProvider;

	private Class<T> mergedClass;

	private PropertyPathAccessor accessor;

	private List<SyncMapping> syncMappings = new ArrayList<SyncMapping>();

	protected MergeFilter defaultFilter = NO_OVERWRITE_FILTER;

	private SyncDeltaModel deltaModel;

	private MatchStrategy<T> matchStrategy;

	private SyncLogger syncLogger;

	public SyncMerger(Class<T> mergedClass, StringKeyProvider<T> keyProvider) {
		this.mergedClass = mergedClass;
		this.keyProvider = keyProvider;
		accessor = new PropertyPathAccessor();
	}

	protected void debugLeft(T left) {
	}

	protected SyncPairAction decideSyncAction(SyncPair<T> pair) {
		if (pair.getLeft() == null) {
			return SyncPairAction.DELETE_RIGHT;
		} else if (pair.getRight() == null) {
			return SyncPairAction.CREATE_RIGHT;
		} else {
			return SyncPairAction.MERGE;
		}
	}

	public SyncMapping define(String propertyName) {
		SyncMapping mapping = new SyncMapping(propertyName);
		syncMappings.add(mapping);
		return mapping;
	}

	protected SyncMapping defineLeft(String propertyName) {
		return this.define(propertyName).mergeFilter(LEFT_IS_DEFINITIVE);
	}

	protected void defineLeftExcluding(String... ignores) {
		List<String> list = new ArrayList<>(Arrays.asList(ignores));
		list.addAll(Arrays.asList("id", "localId", "propertyChangeListeners",
				"class"));
		List<PropertyDescriptor> sortedPropertyDescriptors = SEUtilities
				.getPropertyDescriptorsSortedByName(mergedClass);
		Stream<PropertyDescriptor> stream = sortedPropertyDescriptors.stream()
				.filter(pd -> !list.contains(pd.getName()))
				.filter(pd -> pd.getReadMethod()
						.getAnnotation(AlcinaTransient.class) == null);
		stream.forEach(pd -> defineLeft(pd.getName()));
	}

	protected SyncMapping defineRight(String propertyName) {
		return this.define(propertyName).mergeFilter(RIGHT_IS_DEFINITIVE);
	}

	protected void defineRightExcluding(String... ignores) {
		List<String> list = new ArrayList<>(Arrays.asList(ignores));
		list.addAll(Arrays.asList("id", "localId", "propertyChangeListeners",
				"class"));
		List<PropertyDescriptor> sortedPropertyDescriptors = SEUtilities
				.getPropertyDescriptorsSortedByName(mergedClass);
		Stream<PropertyDescriptor> stream = sortedPropertyDescriptors.stream()
				.filter(pd -> !list.contains(pd.getName()))
				.filter(pd -> pd.getReadMethod()
						.getAnnotation(AlcinaTransient.class) == null);
		stream.forEach(pd -> defineRight(pd.getName()));
	}

	protected SyncMappingWithLog defineWithLog(String propertyName,
			Function<T, Object[]> propertyKeyProvider) {
		SyncMappingWithLog mapping = new SyncMappingWithLog(propertyName,
				propertyKeyProvider);
		syncMappings.add(mapping);
		return mapping;
	}

	protected void ensureLeftWriteable(SyncPair<T> pair) {
		Object left = pair.getLeft().getObject();
		if (left instanceof Entity) {
			Entity entity = (Entity) left;
			if (entity.domain().isNonDomain()) {
			} else {
				entity = entity.domain().detachedVersion();
			}
			pair.getLeft().setObject(entity);
		}
	}

	protected void ensureRightWriteable(SyncPair<T> pair) {
		Object right = pair.getRight().getObject();
		if (right instanceof Entity) {
			Entity entity = (Entity) right;
			if (entity.domain().isNonDomain()) {
			} else {
				entity = entity.domain().detachedVersion();
			}
			pair.getRight().setObject(entity);
		}
	}

	protected Predicate<T> getIgnoreAmbiguityForReportingFilter() {
		return CommonUtils.predicateTrue();
	}

	public MatchStrategy<T> getMatchStrategy() {
		return this.matchStrategy;
	}

	public Class<T> getMergedClass() {
		return this.mergedClass;
	}

	public String getName() {
		String simpleName = getClass().getSimpleName();
		String regex = "([A-Z][a-z]+)([A-Z][a-z]+)([A-Z][a-z]+)Merger";
		if (simpleName.matches(regex)) {
			return simpleName.replaceFirst(regex, "$1 >> $2 :: $3");
		} else {
			return simpleName;
		}
	}

	public SyncLogger getSyncLogger() {
		return this.syncLogger;
	}

	protected boolean ignoreElementsWithAmbiguity() {
		return false;
	}

	public void maybeRegister(Object left, Object right) {
		// if you'd like to TM-record object modifications
	}

	public void merge(Collection<T> leftItems, Collection<T> rightItems,
			SyncDeltaModel deltaModel, Logger logger) {
		if (matchStrategy == null) {
			matchStrategy = new KeyMatchStrategy<>(leftItems, rightItems,
					keyProvider);
		}
		syncLogger = new SyncLogger();
		this.deltaModel = deltaModel;
		LinkedHashSet<T> unmatchedRight = new LinkedHashSet<T>(rightItems);
		for (T left : leftItems) {
			debugLeft(left);
			SyncItemMatch<T> itemMatch = matchStrategy.getRight(left);
			SyncPair pair = null;
			if (itemMatch.currentSyncStatus == SyncItemLogStatus.CATEGORY_IGNORED) {
				itemMatch.logMerge("ignore - category ignored");
				syncLogger.log(itemMatch, null);
			} else if (itemMatch.ambiguous) {
				itemMatch.logMerge("ignore - ambiguous correspondent");
				syncLogger.log(itemMatch, null);
			} else {
				T right = itemMatch.right;
				if (right == null) {
					pair = new SyncPair(left, right, keyProvider,
							SyncPairAction.CREATE_RIGHT, itemMatch);
					itemMatch.logMerge("create right - no right correspondent");
				} else {
					pair = new SyncPair(left, right, keyProvider,
							SyncPairAction.MERGE, itemMatch);
					itemMatch.logMerge("merge - matching correspondent");
				}
				mergePair(pair);
				deltaModel.getDeltas().add(mergedClass, pair);
				unmatchedRight.remove(right);
				syncLogger.log(itemMatch, pair);
			}
		}
		unmatchedRight.removeAll(matchStrategy.getAmbiguousRightElements());
		for (T right : unmatchedRight) {
			SyncItemMatch<T> itemMatch = new SyncItemMatch<>();
			itemMatch.right = right;
			SyncPair pair = new SyncPair(null, right, keyProvider,
					SyncPairAction.CREATE_LEFT, itemMatch);
			itemMatch.logMerge("create left - no right correspondent");
			syncLogger.log(itemMatch, pair);
			mergePair(pair);
			deltaModel.getDeltas().add(mergedClass, pair);
		}
		matchStrategy.log(getIgnoreAmbiguityForReportingFilter(), logger,
				mergedClass);
	}

	protected boolean mergePair(SyncPair<T> pair) {
		SyncPairAction action = decideSyncAction(pair);
		if (action == null || action == SyncPairAction.IGNORE) {
			if (action == SyncPairAction.IGNORE) {
				pair.setAction(SyncPairAction.IGNORE);
			}
			return false;
		}
		switch (action) {
		case DELETE_LEFT:
		case DELETE_RIGHT:
			return true;
		case CREATE_LEFT:
		case CREATE_RIGHT:
			KeyedObject newKo = new KeyedObject<T>();
			try {
				newKo.setKeyProvider(keyProvider);
				if (action == SyncPairAction.CREATE_LEFT) {
					T newInstance = (T) Reflections
							.newInstance(Domain.resolveEntityClass(
									pair.getRight().getObject().getClass()));
					newInstance = postCreateInstance(newInstance, true);
					newKo.setObject(newInstance);
					pair.setLeft(newKo);
				} else {
					T newInstance = (T) Reflections
							.newInstance(Domain.resolveEntityClass(
									pair.getLeft().getObject().getClass()));
					newInstance = postCreateInstance(newInstance, false);
					newKo.setObject(newInstance);
					pair.setRight(newKo);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			break;
		}
		switch (action) {
		case CREATE_RIGHT:
		case MERGE:
		case DELETE_RIGHT:
			ensureRightWriteable(pair);
			break;
		}
		switch (action) {
		case CREATE_LEFT:
		case MERGE:
		case DELETE_LEFT:
			ensureLeftWriteable(pair);
			break;
		}
		for (SyncMapping mapping : syncMappings(pair)) {
			mapping.merge(pair.getLeft().getObject(),
					pair.getRight().getObject());
		}
		postMerge(pair);
		return true;
	}

	protected T postCreateInstance(T newInstance, boolean createLeft) {
		return newInstance;
	}

	protected void postMerge(SyncPair<T> merged) {
	}

	public void setMatchStrategy(MatchStrategy<T> matchStrategy) {
		this.matchStrategy = matchStrategy;
	}

	protected List<SyncMapping> syncMappings(SyncPair<T> pair) {
		return syncMappings;
	}

	public boolean validate(Collection<T> leftCollection,
			Collection<T> rightCollection, Logger logger) {
		return true;
	}

	public boolean wasIncomplete() {
		List<SyncLoggerRow> list = syncLogger.rows.stream().filter(
				slr -> slr.provideHadIssue(ignoreElementsWithAmbiguity()))
				.collect(Collectors.toList());
		boolean incomplete = list.size() > 0;
		if (incomplete) {
			Ax.out("Incomplete rows");
			Ax.out(list);
		}
		return incomplete;
	}

	protected abstract class CustomMergeFilter implements MergeFilter<T> {
		@Override
		public boolean allowLeftToRight(T left, T right, Object leftProp,
				Object rightProp) {
			return false;
		}

		@Override
		public boolean allowRightToLeft(T left, T right, Object leftProp,
				Object rightProp) {
			return false;
		}

		@Override
		public boolean isCustom() {
			return true;
		}

		@Override
		public abstract void mapCustom(T left, T right);
	}

	static class FirstAndAllLookup<T> {
		Multimap<String, List<T>> firstKeyLookup = new Multimap<String, List<T>>();

		Multimap<String, List<T>> allKeyLookup = new Multimap<String, List<T>>();

		public FirstAndAllLookup(Collection<T> leftItems,
				StringKeyProvider<T> keyProvider) {
			for (T t : leftItems) {
				firstKeyLookup.add(keyProvider.firstKey(t), t);
				for (String key : keyProvider.allKeys(t)) {
					allKeyLookup.add(key, t);
				}
			}
		}

		public String allLocators(List<String> allKeys) {
			Collection forKeys = allKeyLookup.getForKeys(allKeys);
			if (forKeys.size() > 5) {
				int size = forKeys.size();
				forKeys = new ArrayList(forKeys).subList(0, 5);
				forKeys.add(String.format("...and %s more", size));
			}
			return CommonUtils.join(forKeys, "\n");
		}

		public boolean isMultipleAll(List<String> allKeys) {
			return allKeyLookup.getForKeys(allKeys).size() > 1;
		}

		public boolean isMultipleFirst(String key) {
			return firstKeyLookup.getAndEnsure(key).size() > 1;
		}
	}

	public static interface MergeFilter<T> {
		public boolean allowLeftToRight(T left, T right, Object leftProp,
				Object rightProp);

		public boolean allowRightToLeft(T left, T right, Object leftProp,
				Object rightProp);

		default boolean isCustom() {
			return false;
		}

		default void mapCustom(T left, T right) {
			throw new UnsupportedOperationException();
		}
	}

	public class SyncMapping {
		protected String propertyName;

		protected MergeFilter filter = defaultFilter;

		public SyncMapping(String propertyName) {
			this.propertyName = propertyName;
		}

		public void merge(Object left, Object right) {
			Object leftProp = accessor.getPropertyValue(left, propertyName);
			Object rightProp = accessor.getPropertyValue(right, propertyName);
			if (filter.isCustom()) {
				filter.mapCustom(left, right);
			} else {
				if (filter.allowLeftToRight(left, right, leftProp, rightProp)) {
					accessor.setPropertyValue(right, propertyName, leftProp);
					rightProp = accessor.getPropertyValue(right, propertyName);
				}
				if (filter.allowRightToLeft(left, right, leftProp, rightProp)) {
					accessor.setPropertyValue(left, propertyName, rightProp);
				}
			}
		}

		public SyncMapping mergeFilter(MergeFilter mergeFilter) {
			this.filter = mergeFilter;
			return this;
		}

		@Override
		public String toString() {
			return Ax.format("[%s]", propertyName);
		}
	}

	public class SyncMappingWithLog extends SyncMapping {
		private Function<T, Object[]> propertyKeyProvider;

		public List<MergeHistory> history = new ArrayList<>();

		public SyncMappingWithLog(String propertyName,
				Function<T, Object[]> propertyKeyProvider) {
			super(propertyName);
			this.propertyKeyProvider = propertyKeyProvider;
			MergeFilter filter = NO_OVERWRITE_FILTER;
		}

		@Override
		public void merge(Object left, Object right) {
			PropertyModificationLog propertyModificationLog = deltaModel
					.getPropertyModificationLog();
			assert left != null && right != null;
			Object[] keys = propertyKeyProvider.apply((T) left);
			List<PropertyModificationLogItem> items = propertyModificationLog
					.itemsFor(new Object[] { keys[0], keys[1], propertyName });
			boolean withoutLog = items.isEmpty() || keys[1] == null
					|| keys[0] == null;
			withoutLog |= ((Entity) left).getId() == 0
					|| ((Entity) right).getId() == 0;
			if (withoutLog) {
				mergeWithoutLog(left, right);
			} else {
				// assume String prop at the moment
				Object leftValue = accessor.getPropertyValue(left,
						propertyName);
				Object rightValue = accessor.getPropertyValue(right,
						propertyName);
				if (!Objects.equals(leftValue, rightValue)) {
					String newStringValue = CommonUtils.last(items).getValue();
					Object value = null;
					boolean write = true;
					if (newStringValue != null) {
						// hijack TM
						DomainTransformEvent event = new DomainTransformEvent();
						event.setNewStringValue(newStringValue);
						event.setValueClass(
								accessor.getPropertyType(
										Reflections.at(getMergedClass())
												.templateInstance(),
										propertyName));
						try {
							value = TransformManager.get()
									.getTargetObject(event, false);
						} catch (Exception e) {
							write = false;
							e.printStackTrace();
						}
					}
					if (write) {
						MergeHistory mergeHistory = new MergeHistory(left,
								right, leftValue, rightValue, value, items);
						mergeHistory.log();
						history.add(mergeHistory);
						maybeRegister(left, right);
						accessor.setPropertyValue(left, propertyName, value);
						accessor.setPropertyValue(right, propertyName, value);
					}
				}
			}
		}

		public void mergeWithoutLog(Object left, Object right) {
			Object leftProp = accessor.getPropertyValue(left, propertyName);
			Object rightProp = accessor.getPropertyValue(right, propertyName);
			if (filter.allowLeftToRight(left, right, leftProp, rightProp)) {
				accessor.setPropertyValue(right, propertyName, leftProp);
				rightProp = accessor.getPropertyValue(right, propertyName);
			}
			if (filter.allowRightToLeft(left, right, leftProp, rightProp)) {
				accessor.setPropertyValue(left, propertyName, rightProp);
			}
		}

		@SuppressWarnings("unused")
		public class MergeHistory {
			private Object left;

			private Object right;

			private Object leftValue;

			private Object rightValue;

			private Object value;

			private List<PropertyModificationLogItem> items;

			public MergeHistory(Object left, Object right, Object leftValue,
					Object rightValue, Object value,
					List<PropertyModificationLogItem> items) {
				this.left = left;
				this.right = right;
				this.leftValue = leftValue;
				this.rightValue = rightValue;
				this.value = value;
				this.items = items;
			}

			private String getMessage() {
				String message = Ax.format(
						"Property merge (left,right) %s %s -> %s", leftValue,
						rightValue, value);
				return message;
			}

			public void log() {
				String message = getMessage();
				// System.out.println(message);
			}

			@Override
			public String toString() {
				return String.format("%s\n%s", getMessage(),
						CommonUtils.joinWithNewlineTab(items));
			}
		}
	}
}
