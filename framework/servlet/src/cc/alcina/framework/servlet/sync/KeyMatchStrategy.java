package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.servlet.sync.SyncMerger.FirstAndAllLookup;

public class KeyMatchStrategy<T> implements MatchStrategy<T> {
	public static final String TOPIC_MERGE_ISSUE = MergeHandler.class.getName()
			+ "." + "TOPIC_MERGE_ISSUE";

	public static TopicSupport<KeyMatchIssue> topicMatchIssue() {
		return new TopicSupport<>(TOPIC_MERGE_ISSUE);
	}

	private StringKeyProvider<T> keyProvider;

	FirstAndAllLookup leftLookup;

	FirstAndAllLookup rightLookup;

	// simplistic - only allow first key matching -
	Set<T> unmatchedRight;

	Map<T, String> ambiguousLeft = new LinkedHashMap<T, String>();

	// keys must match uniquely -- if not, fix manually
	Map<T, String> ambiguousRight = new LinkedHashMap<T, String>();

	public KeyMatchStrategy(Collection<T> leftItems, Collection<T> rightItems,
			StringKeyProvider<T> keyProvider) {
		this.keyProvider = keyProvider;
		leftLookup = new FirstAndAllLookup(leftItems, keyProvider);
		rightLookup = new FirstAndAllLookup(rightItems, keyProvider);
		unmatchedRight = new LinkedHashSet<T>(rightItems);
	}

	@Override
	public Collection<T> getAmbiguousRightElements() {
		return ambiguousRight.keySet();
	}

	@Override
	public SyncItemMatch<T> getRight(T left) {
		T right = null;
		List<String> ambiguous = new ArrayList();
		String key = keyProvider.firstKey(left);
		List<String> allKeys = keyProvider.allKeys(left);
		if (leftLookup.isMultipleAll(allKeys)) {
			ambiguous.add(String.format("multiple left matches for %s:\n%s",
					allKeys, leftLookup.allLocators(allKeys)));
		}
		if (rightLookup.isMultipleAll(allKeys)) {
			String message = String.format("multiple right matches for %s:\n%s",
					allKeys, rightLookup.allLocators(allKeys));
			ambiguous.add(message);
			Collection ambiguousMatched = new ArrayList(
					rightLookup.allKeyLookup.getForKeys(allKeys));
			topicMatchIssue().publish(
					new KeyMatchIssue(left, ambiguousMatched, message));
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
					List<T> leftMatched = leftLookup.allKeyLookup.get(firstKey);
					ambiguous.add(String.format(
							"higher precedence right matches for %s:"
									+ " %s \nRight object matched: %s"
									+ "\nAlt left object matched: %s",
							allKeys, firstKey, test,
							CommonUtils.first(leftMatched)));
				}
			}
		}
		SyncItemMatch<T> result = new SyncItemMatch<>();
		result.left = left;
		// very pessimistic
		if (ambiguous.size() > 0) {
			String message = String.format("%s\n", CommonUtils
					.padLinesLeft(CommonUtils.join(ambiguous, "\n"), "\t\t"));
			for (T t : (Collection<T>) leftLookup.allKeyLookup
					.getForKeys(allKeys)) {
				ambiguousLeft.put(t, message);
			}
			for (T t : (Collection<T>) rightLookup.allKeyLookup
					.getForKeys(allKeys)) {
				ambiguousRight.put(t, message);
			}
			result.ambiguous = true;
		} else {
			Collection<T> rightCorresponding = rightLookup.allKeyLookup
					.getForKeys(allKeys);
			right = CommonUtils.first(rightCorresponding);
			result.right = right;
		}
		return result;
	}

	@Override
	public void log(CollectionFilter<T> ignoreAmbiguityForReportingFilter,
			Logger logger, Class<T> mergedClass) {
		CollectionFilters.filterInPlace(ambiguousLeft.keySet(),
				ignoreAmbiguityForReportingFilter);
		CollectionFilters.filterInPlace(ambiguousRight.keySet(),
				ignoreAmbiguityForReportingFilter);
		if (ambiguousLeft.isEmpty() && ambiguousRight.isEmpty()) {
			logger.info(
					String.format("Merge [%s]", mergedClass.getSimpleName()));
			return;
		}
		logger.info(String.format(
				"Merge [%s]: %sambiguous left:\t%-6s\tambiguous right:\t%-6s",
				mergedClass.getSimpleName(),
				CommonUtils.padStringLeft("",
						25 - mergedClass.getSimpleName().length(), " "),
				ambiguousLeft.size(), ambiguousRight.size()));
		// logger.debug(String.format(
		// "Merge [%s]: ambiguous left:\n\t%s\nambiguous right:\n\t%s\n\n",
		// mergedClass.getSimpleName(),
		// CommonUtils.joinWithNewlineTab(
		// CommonUtils.flattenMap(ambiguousLeft)),
		// CommonUtils.joinWithNewlineTab(
		// CommonUtils.flattenMap(ambiguousRight))));
	}

	public static class KeyMatchIssue<T> {
		public T left;

		public Collection<T> ambiguousMatched;

		public String message;

		public KeyMatchIssue(T left, Collection<T> ambiguousMatched,
				String message) {
			this.left = left;
			this.ambiguousMatched = ambiguousMatched;
			this.message = message;
		}
	}
}