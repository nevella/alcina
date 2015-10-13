package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
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
	private StringKeyProvider<T> keyProvider;

	private Class<T> mergedClass;

	public SyncMerger(Class<T> mergedClass, StringKeyProvider<T> keyProvider) {
		this.mergedClass = mergedClass;
		this.keyProvider = keyProvider;
		propertyAccessor = new JvmPropertyAccessor();
	}

	private PropertyAccessor propertyAccessor;

	private List<SyncMapping> syncMappings = new ArrayList<SyncMapping>();

	public static interface MergeFilter {
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp);

		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp);
	}

	public static MergeFilter RIGHT_IS_DEFINITIVE = new MergeFilter() {
		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return true;
		}

		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return false;
		}
	};

	public static MergeFilter LEFT_IS_DEFINITIVE = new MergeFilter() {
		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return false;
		}

		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return true;
		}
	};

	protected MergeFilter defaultFilter = NO_OVERWRITE_FILTER;

	public static final MergeFilter NO_OVERWRITE_FILTER = new MergeFilter() {
		@Override
		public boolean allowRightToLeft(Object left, Object right,
				Object leftProp, Object rightProp) {
			return leftProp == null;
		}

		@Override
		public boolean allowLeftToRight(Object left, Object right,
				Object leftProp, Object rightProp) {
			return rightProp == null;
		}
	};

	public class SyncMapping {
		private String propertyName;

		public SyncMapping(String propertyName) {
			this.propertyName = propertyName;
		}

		private MergeFilter mergeFilter;

		public void merge(Object left, Object right) {
			MergeFilter filter = mergeFilter != null ? mergeFilter
					: defaultFilter;
			Object leftProp = propertyAccessor.getPropertyValue(left,
					propertyName);
			Object rightProp = propertyAccessor.getPropertyValue(right,
					propertyName);
			if (filter.allowLeftToRight(left, right, leftProp, rightProp)) {
				propertyAccessor
						.setPropertyValue(right, propertyName, leftProp);
				rightProp = propertyAccessor.getPropertyValue(right,
						propertyName);
			}
			if (filter.allowRightToLeft(left, right, leftProp, rightProp)) {
				propertyAccessor
						.setPropertyValue(left, propertyName, rightProp);
			}
		}

		public SyncMapping mergeFilter(MergeFilter mergeFilter) {
			this.mergeFilter = mergeFilter;
			return this;
		}
	}

	public SyncMapping define(String propertyName) {
		SyncMapping mapping = new SyncMapping(propertyName);
		syncMappings.add(mapping);
		return mapping;
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
		if (syncType == null||syncType==SyncPairAction.IGNORE) {
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
			mapping.merge(pair.getLeft().getObject(), pair.getRight()
					.getObject());
		}
		return true;
	}

	class FirstAndAllLookup {
		public FirstAndAllLookup(Collection<T> leftItems) {
			for (T t : leftItems) {
				firstKeyLookup.add(keyProvider.firstKey(t), t);
				for (String key : keyProvider.allKeys(t)) {
					allKeyLookup.add(key, t);
				}
			}
		}

		Multimap<String, List<T>> firstKeyLookup = new Multimap<String, List<T>>();

		Multimap<String, List<T>> allKeyLookup = new Multimap<String, List<T>>();

		public boolean isMultipleFirst(String key) {
			return firstKeyLookup.getAndEnsure(key).size() > 1;
		}

		public boolean isMultipleAll(List<String> allKeys) {
			return allKeyLookup.getForKeys(allKeys).size() > 1;
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
	}

	public void merge(Collection<T> leftItems, Collection<T> rightItems,
			SyncDeltaModel deltaModel, Logger logger) {
		FirstAndAllLookup leftLookup = new FirstAndAllLookup(leftItems);
		FirstAndAllLookup rightLookup = new FirstAndAllLookup(rightItems);
		// simplistic - only allow first key matching -
		Set<T> unmatchedRight = new LinkedHashSet<T>(rightItems);
		Map<T, String> ambiguousLeft = new LinkedHashMap<T, String>();
		Map<T, String> ambiguousRight = new LinkedHashMap<T, String>();
		// keys must match uniquely -- if not, fix manually
		for (T left : leftItems) {
			List<String> ambiguous = new ArrayList();
			T right = null;
			String key = keyProvider.firstKey(left);
			List<String> allKeys = keyProvider.allKeys(left);
			if (leftLookup.isMultipleAll(allKeys)) {
				ambiguous.add(String.format(
						"multiple left matches for %s:\n%s", allKeys,
						leftLookup.allLocators(allKeys)));
			}
			if (rightLookup.isMultipleAll(allKeys)) {
				ambiguous.add(String.format(
						"multiple right matches for %s:\n%s", allKeys,
						rightLookup.allLocators(allKeys)));
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
				String message = String.format(
						"%s\n",
						CommonUtils.padLinesLeft(
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
				mergedClass.getSimpleName(), CommonUtils.padStringLeft("",
						25 - mergedClass.getSimpleName().length(), " "),
				ambiguousLeft.size(), ambiguousRight.size()));
		logger.debug(String
				.format("Merge [%s]: ambiguous left:\n\t%s\nambiguous right:\n\t%s\n\n",
						mergedClass.getSimpleName(), CommonUtils
								.joinWithNewlineTab(CommonUtils
										.flattenMap(ambiguousLeft)),
						CommonUtils.joinWithNewlineTab(CommonUtils
								.flattenMap(ambiguousRight))));
	}

	protected CollectionFilter<T> getIgnoreAmbiguityForReportingFilter() {
		return CollectionFilters.PASSTHROUGH_FILTER;
	}

	public Class<T> getMergedClass() {
		return this.mergedClass;
	}

	protected SyncMapping defineRight(String propertyName) {
		return this.define(propertyName).mergeFilter(RIGHT_IS_DEFINITIVE);
	}

	protected SyncMapping defineLeft(String propertyName) {
		return this.define(propertyName).mergeFilter(LEFT_IS_DEFINITIVE);
	}
}
