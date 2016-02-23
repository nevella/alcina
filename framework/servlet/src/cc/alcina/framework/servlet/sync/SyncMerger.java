package cc.alcina.framework.servlet.sync;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLog;
import cc.alcina.framework.common.client.sync.property.PropertyModificationLogItem;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.JvmPropertyAccessor;
import cc.alcina.framework.servlet.sync.SyncPair.SyncPairAction;

/**
 * Target is 'right' - so if left doesn't exist, will be a delete - if right
 * doesn't, a create
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

	private StringKeyProvider<T> keyProvider;

	private Class<T> mergedClass;

	private PropertyAccessor propertyAccessor;

	private List<SyncMapping> syncMappings = new ArrayList<SyncMapping>();

	protected MergeFilter defaultFilter = NO_OVERWRITE_FILTER;

	private SyncDeltaModel deltaModel;

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

	public Class<T> getMergedClass() {
		return this.mergedClass;
	}

	public void merge(Collection<T> leftItems, Collection<T> rightItems,
			SyncDeltaModel deltaModel, Logger logger) {
		this.deltaModel = deltaModel;
		FirstAndAllLookup leftLookup = new FirstAndAllLookup(leftItems);
		FirstAndAllLookup rightLookup = new FirstAndAllLookup(rightItems);
		// simplistic - only allow first key matching -
		Set<T> unmatchedRight = new LinkedHashSet<T>(rightItems);
		Map<T, String> ambiguousLeft = new LinkedHashMap<T, String>();
		Map<T, String> ambiguousRight = new LinkedHashMap<T, String>();
		// keys must match uniquely -- if not, fix manually
		for (T left : leftItems) {
			debugLeft(left);
			List<String> ambiguous = new ArrayList();
			T right = null;
			String key = keyProvider.firstKey(left);
			List<String> allKeys = keyProvider.allKeys(left);
			if (leftLookup.isMultipleAll(allKeys)) {
				ambiguous.add(String.format("multiple left matches for %s:\n%s",
						allKeys, leftLookup.allLocators(allKeys)));
			}
			if (rightLookup.isMultipleAll(allKeys)) {
				ambiguous
						.add(String.format("multiple right matches for %s:\n%s",
								allKeys, rightLookup.allLocators(allKeys)));
			}
			if (ambiguous.isEmpty()) {
				// check, say, left has distinct firstkey to right - note,
				// right.firstKey is enough check...think about it
				Collection<T> rightForKeys = rightLookup.allKeyLookup
						.getForKeys(allKeys);
				for (T test : rightForKeys) {
					String firstKey = keyProvider.firstKey(test);
					if (!allKeys.contains(firstKey)
							&& leftLookup.allKeyLookup.containsKey(firstKey)) {
						List<T> leftMatched = leftLookup.allKeyLookup
								.get(firstKey);
						ambiguous.add(String.format(
								"higher precedence right matches for %s:"
										+ " %s \nRight object matched: %s"
										+ "\nAlt left object matched: %s",
								allKeys, firstKey, test,
								CommonUtils.first(leftMatched)));
					}
				}
			}
			// very pessimistic
			if (ambiguous.size() > 0) {
				String message = String.format("%s\n", CommonUtils.padLinesLeft(
						CommonUtils.join(ambiguous, "\n"), "\t\t"));
				for (T t : (Collection<T>) leftLookup.allKeyLookup
						.getForKeys(allKeys)) {
					ambiguousLeft.put(t, message);
				}
				for (T t : (Collection<T>) rightLookup.allKeyLookup
						.getForKeys(allKeys)) {
					ambiguousRight.put(t, message);
				}
			} else {
				Collection<T> rightCorresponding = rightLookup.allKeyLookup
						.getForKeys(allKeys);
				right = CommonUtils.first(rightCorresponding);
				SyncPair pair = null;
				if (right == null) {
					pair = new SyncPair(left, right, keyProvider,
							SyncPairAction.CREATE_RIGHT);
				} else {
					pair = new SyncPair(left, right, keyProvider,
							SyncPairAction.MERGE);
				}
				mergePair(pair);
				deltaModel.getDeltas().add(mergedClass, pair);
				unmatchedRight.remove(right);
			}
		}
		unmatchedRight.removeAll(ambiguousRight.keySet());
		for (T right : unmatchedRight) {
			SyncPair pair = new SyncPair(null, right, keyProvider,
					SyncPairAction.CREATE_LEFT);
			mergePair(pair);
			deltaModel.getDeltas().add(mergedClass, pair);
		}
		CollectionFilters.filterInPlace(ambiguousLeft.keySet(),
				getIgnoreAmbiguityForReportingFilter());
		CollectionFilters.filterInPlace(ambiguousRight.keySet(),
				getIgnoreAmbiguityForReportingFilter());
		logger.info(String.format(
				"Merge [%s]: %sambiguous left:\t%-6s\tambiguous right:\t%-6s",
				mergedClass.getSimpleName(),
				CommonUtils.padStringLeft("",
						25 - mergedClass.getSimpleName().length(), " "),
				ambiguousLeft.size(), ambiguousRight.size()));
		logger.debug(String.format(
				"Merge [%s]: ambiguous left:\n\t%s\nambiguous right:\n\t%s\n\n",
				mergedClass.getSimpleName(),
				CommonUtils.joinWithNewlineTab(
						CommonUtils.flattenMap(ambiguousLeft)),
				CommonUtils.joinWithNewlineTab(
						CommonUtils.flattenMap(ambiguousRight))));
	}

	protected void debugLeft(T left) {
	}

	protected SyncMapping defineLeft(String propertyName) {
		return this.define(propertyName).mergeFilter(LEFT_IS_DEFINITIVE);
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

	protected CollectionFilter<T> getIgnoreAmbiguityForReportingFilter() {
		return CollectionFilters.PASSTHROUGH_FILTER;
	}

	protected SyncPairAction getSyncType(SyncPair<T> pair) {
		if (pair.getLeft() == null) {
			return SyncPairAction.DELETE_RIGHT;
		} else if (pair.getRight() == null) {
			return SyncPairAction.CREATE_RIGHT;
		} else {
			return SyncPairAction.MERGE;
		}
	}

	protected boolean mergePair(SyncPair<T> pair) {
		SyncPairAction syncType = getSyncType(pair);
		if (syncType == null || syncType == SyncPairAction.IGNORE) {
			return false;
		}
		pair.setAction(syncType);
		switch (syncType) {
		case DELETE_LEFT:
		case DELETE_RIGHT:
			return true;
		case CREATE_LEFT:
		case CREATE_RIGHT:
			KeyedObject newKo = new KeyedObject<T>();
			try {
				newKo.setKeyProvider(keyProvider);
				if (syncType == SyncPairAction.CREATE_LEFT) {
					newKo.setObject(pair.getRight().getObject().getClass()
							.newInstance());
					pair.setLeft(newKo);
				} else {
					newKo.setObject(pair.getLeft().getObject().getClass()
							.newInstance());
					pair.setRight(newKo);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			break;
		}
		for (SyncMapping mapping : syncMappings) {
			mapping.merge(pair.getLeft().getObject(),
					pair.getRight().getObject());
		}
		return true;
	}

	public static interface MergeFilter {
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp);

		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp);
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

		public SyncMapping mergeFilter(MergeFilter mergeFilter) {
			this.filter = mergeFilter;
			return this;
		}
	}

	public class SyncMappingWithLog extends SyncMapping {
		private Function<T, Object[]> propertyKeyProvider;

		public SyncMappingWithLog(String propertyName,
				Function<T, Object[]> propertyKeyProvider) {
			super(propertyName);
			this.propertyKeyProvider = propertyKeyProvider;
			MergeFilter filter = NO_OVERWRITE_FILTER;
		}

		public List<MergeHistory> history = new ArrayList<>();

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

			private String getMessage() {
				String message = CommonUtils.formatJ(
						"Property merge (left,right) %s %s -> %s", leftValue,
						rightValue, value);
				return message;
			}

			@Override
			public String toString() {
				return String.format("%s\n%s", getMessage(),
						CommonUtils.joinWithNewlineTab(items));
			}
		}

		public void merge(Object left, Object right) {
			PropertyModificationLog propertyModificationLog = deltaModel
					.getPropertyModificationLog();
			assert left != null && right != null;
			Object[] keys = propertyKeyProvider.apply((T) left);
			List<PropertyModificationLogItem> items = propertyModificationLog
					.itemsFor(new Object[] { keys[0], keys[1], propertyName });
			if (Objects.equals(keys[1], "JCT-243017")) {
				int debug = 3;
			}
			boolean withoutLog = items.isEmpty() || keys[1] == null
					|| keys[0] == null;
			withoutLog |= ((HasIdAndLocalId) left).getId() == 0
					|| ((HasIdAndLocalId) right).getId() == 0;
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
						if (value == null) {
							int debug = 3;
						}
						if (Objects.equals(value, rightValue)
								&& propertyName.equals("email")) {
							int debug = 3;
						}
						history.add(mergeHistory);
						maybeRegister(left,right);
						
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
	}

	class FirstAndAllLookup {
		Multimap<String, List<T>> firstKeyLookup = new Multimap<String, List<T>>();

		Multimap<String, List<T>> allKeyLookup = new Multimap<String, List<T>>();

		public FirstAndAllLookup(Collection<T> leftItems) {
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

	public void maybeRegister(Object left, Object right) {
		//if you'd like to TM-record object modifications
	}
}
