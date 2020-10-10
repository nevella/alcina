package cc.alcina.framework.servlet.sync;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLog;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLogItem;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.transform.JvmPropertyAccessor;
import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogStatus;
import cc.alcina.framework.servlet.sync.SyncPair.SyncPairAction;

/**
 * Target is 'right' - so if left doesn't exist, will be a delete - if right
 * doesn't, a create
 * 
 * Note that left *must* have distinct keys (if using key matcher strategy)
 * 
 * @author nick@alcina.cc
 * 
 *         In general, when adding a merge field, make sure that the object's
 *         equivalentTo checks the new field
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

	private PropertyAccessor propertyAccessor;

	private List<SyncMapping> syncMappings = new ArrayList<SyncMapping>();

	protected MergeFilter defaultFilter = NO_OVERWRITE_FILTER;

	private SyncDeltaModel deltaModel;

	private MatchStrategy<T> matchStrategy;

	private SyncLogger syncLogger;

	public SyncMerger(Class<T> mergedClass, StringKeyProvider<T> keyProvider) {
		this.mergedClass = mergedClass;
		this.keyProvider = keyProvider;
		propertyAccessor = new JvmPropertyAccessor();
	}

	public SyncMapping define(String propertyName) {
		SyncMapping mapping = new SyncMapping(propertyName);
		syncMappings.add(mapping);
		return mapping;
	}

	public MatchStrategy<T> getMatchStrategy() {
		return this.matchStrategy;
	}

	public Class<T> getMergedClass() {
		return this.mergedClass;
	}

	public SyncLogger getSyncLogger() {
		return this.syncLogger;
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

	public void setMatchStrategy(MatchStrategy<T> matchStrategy) {
		this.matchStrategy = matchStrategy;
	}

	public boolean validate(Collection<T> leftCollection,
			Collection<T> rightCollection, Logger logger) {
		return true;
	}

	public boolean wasIncomplete() {
		return syncLogger.rows.stream().anyMatch(
				slr -> slr.provideHadIssue(ignoreElementsWithAmbiguity()));
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

	protected SyncMapping defineLeft(String propertyName) {
		return this.define(propertyName).mergeFilter(LEFT_IS_DEFINITIVE);
	}

	protected void defineLeftExcluding(String... ignores) {
		List<String> list = new ArrayList<>(Arrays.asList(ignores));
		list.addAll(Arrays.asList("id", "localId", "propertyChangeListeners",
				"class"));
		List<PropertyDescriptor> sortedPropertyDescriptors = SEUtilities
				.getSortedPropertyDescriptors(mergedClass);
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
				.getSortedPropertyDescriptors(mergedClass);
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

	protected CollectionFilter<T> getIgnoreAmbiguityForReportingFilter() {
		return CollectionFilters.PASSTHROUGH_FILTER;
	}

	protected boolean ignoreElementsWithAmbiguity() {
		return false;
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
					T newInstance = (T) Domain
							.resolveEntityClass(
									pair.getRight().getObject().getClass())
							.newInstance();
					newInstance = postCreateInstance(newInstance, true);
					newKo.setObject(newInstance);
					pair.setLeft(newKo);
				} else {
					T newInstance = (T) Domain
							.resolveEntityClass(
									pair.getLeft().getObject().getClass())
							.newInstance();
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
		return true;
	}

	protected T postCreateInstance(T newInstance, boolean createLeft) {
		return newInstance;
	}

	protected List<SyncMapping> syncMappings(SyncPair<T> pair) {
		return syncMappings;
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
			Object leftProp = propertyAccessor.getPropertyValue(left,
					propertyName);
			Object rightProp = propertyAccessor.getPropertyValue(right,
					propertyName);
			if (filter.isCustom()) {
				filter.mapCustom(left, right);
			} else {
				if (filter.allowLeftToRight(left, right, leftProp, rightProp)) {
					propertyAccessor.setPropertyValue(right, propertyName,
							leftProp);
					rightProp = propertyAccessor.getPropertyValue(right,
							propertyName);
				}
				if (filter.allowRightToLeft(left, right, leftProp, rightProp)) {
					propertyAccessor.setPropertyValue(left, propertyName,
							rightProp);
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
				Object leftValue = propertyAccessor.getPropertyValue(left,
						propertyName);
				Object rightValue = propertyAccessor.getPropertyValue(right,
						propertyName);
				if (!Objects.equals(leftValue, rightValue)) {
					String newStringValue = CommonUtils.last(items).getValue();
					Object value = null;
					boolean write = true;
					if (newStringValue != null) {
						// hijack TM
						DomainTransformEvent event = new DomainTransformEvent();
						event.setNewStringValue(newStringValue);
						event.setValueClass(propertyAccessor.getPropertyType(
								getMergedClass(), propertyName));
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
						propertyAccessor.setPropertyValue(left, propertyName,
								value);
						propertyAccessor.setPropertyValue(right, propertyName,
								value);
					}
				}
			}
		}

		public void mergeWithoutLog(Object left, Object right) {
			Object leftProp = propertyAccessor.getPropertyValue(left,
					propertyName);
			Object rightProp = propertyAccessor.getPropertyValue(right,
					propertyName);
			if (filter.allowLeftToRight(left, right, leftProp, rightProp)) {
				propertyAccessor.setPropertyValue(right, propertyName,
						leftProp);
				rightProp = propertyAccessor.getPropertyValue(right,
						propertyName);
			}
			if (filter.allowRightToLeft(left, right, leftProp, rightProp)) {
				propertyAccessor.setPropertyValue(left, propertyName,
						rightProp);
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

			public void log() {
				String message = getMessage();
				// System.out.println(message);
			}

			@Override
			public String toString() {
				return String.format("%s\n%s", getMessage(),
						CommonUtils.joinWithNewlineTab(items));
			}

			private String getMessage() {
				String message = Ax.format(
						"Property merge (left,right) %s %s -> %s", leftValue,
						rightValue, value);
				return message;
			}
		}
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
}
