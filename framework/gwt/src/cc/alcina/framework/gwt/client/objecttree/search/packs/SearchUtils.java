package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.datepicker.client.CalendarUtil;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.search.TextCriterion.TextCriterionType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.DateUtil;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasDisplayName;

public class SearchUtils {
	static SearchUtilsIdsHelper idsHelper;

	static SearchUtilsRegExpHelper regexpHelper;

	public static final String IDS_REGEX = "(?:ids?: ?)?(-?[0-9][0-9]*,? *)+";

	public static final String REGEX_REGEX = "(?:regex:)(.+)";
	static {
		idsHelper = Registry.impl(SearchUtilsIdsHelper.class);
		regexpHelper = Registry.impl(SearchUtilsRegExpHelper.class);
	}

	public static boolean containsIgnoreCase(String text,
			List<String> strings) {
		return new SearchTextMatcher().targets(strings).contains(text);
	}

	public static boolean containsIgnoreCase(String contains,
			String contained) {
		if (contains == null || contained == null) {
			return false;
		}
		String lc1 = contains.toLowerCase();
		String lc2 = contained.toLowerCase();
		return lc1.contains(lc2);
	}

	public static boolean containsIgnoreCase(String text, String... strings) {
		return new SearchTextMatcher().targets(strings).contains(text);
	}

	public static boolean equalsIgnoreCase(String text, List<String> strings) {
		return new SearchTextMatcher().targets(strings).equalTo(text);
	}

	public static boolean equalsIgnoreCase(String text, String... strings) {
		return new SearchTextMatcher().targets(strings).equalTo(text);
	}

	public static boolean equalsIgnoreCase(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return false;
		}
		String lc1 = s1.toLowerCase();
		String lc2 = s2.toLowerCase();
		return lc1.equals(lc2);
	}

	public static Set<Long> idsTextToSet(String idsText) {
		return TransformManager
				.idListToLongSet(idsText.replaceFirst("ids?: ?", ""));
	}

	public static boolean matches(String query, Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof HasDisplayName) {
			if (containsIgnoreCase(((HasDisplayName) o).displayName(), query)) {
				return true;
			}
		}
		if (o instanceof Enum) {
			if (matchesEnum(query, (Enum) o)) {
				return true;
			}
		}
		return containsIgnoreCase(o.toString(), query);
	}

	public static boolean matchesEnum(String query, Enum e) {
		if (e == null) {
			return false;
		}
		if (containsIgnoreCase(e.toString(), query)) {
			return true;
		}
		if (containsIgnoreCase(e.toString().replace('_', ' '), query)) {
			return true;
		}
		return false;
	}

	public static boolean matchesId(String query, Entity entity) {
		if (matchesIds(query, entity)) {
			return true;
		}
		return entity != null && toId(query) == entity.getId();
	}

	private static boolean matchesIds(String query, Entity entity) {
		if (idsHelper.matches(query, entity)) {
			return true;
		}
		if (regexpHelper.matches(query, entity)) {
			return true;
		}
		return false;
	}

	public static boolean matchesIdsQuery(String query) {
		return query.matches(IDS_REGEX);
	}

	public static boolean matchesRegexQuery(String query) {
		return query.matches(REGEX_REGEX);
	}

	public static long toId(String s) {
		return s == null || !s.matches("(?:id:)?[0-9]+") ? Integer.MIN_VALUE
				: Long.parseLong(s.replaceFirst("(?:id:)?([0-9]+)", "$1"));
		// return stringIdLookup.get(text);
	}

	public static void toTextSearch(SearchDefinition def, String text) {
		RegExp regExp = RegExp.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})", "g");
		MatchResult result;
		Date date = null;
		if ((result = regExp.exec(text)) != null) {
			date = DateUtil.yearAsDate(Integer.parseInt(result.getGroup(3)));
			CalendarUtil.addMonthsToDate(date,
					Integer.parseInt(result.getGroup(2)) - 1);
			CalendarUtil.addDaysToDate(date,
					Integer.parseInt(result.getGroup(1)) - 1);
		}
		if (date != null) {
			for (SearchCriterion criterion : def.allCriteria()) {
				if (criterion instanceof DateCriterion) {
					DateCriterion dateCriterion = (DateCriterion) criterion;
					if (dateCriterion.getDirection() == Direction.ASCENDING) {
						Date d2 = new Date(date.getTime());
						CalendarUtil.addDaysToDate(d2, -2);
						dateCriterion.setValue(d2);
					} else {
						Date d2 = new Date(date.getTime());
						CalendarUtil.addDaysToDate(d2, +2);
						dateCriterion.setValue(d2);
					}
				}
			}
		} else {
			TextCriterion txtCriterion = new TextCriterion();
			txtCriterion.setValue(text);
			txtCriterion.setTextCriterionType(TextCriterionType.CONTAINS);
			def.addCriterionToSoleCriteriaGroup(txtCriterion);
		}
	}

	public static class SearchTextMatcher {
		private String[] targets;

		public boolean contains(String text) {
			if (text == null) {
				return false;
			}
			text = text.toLowerCase();
			String[] texts = text.split(" or ");
			for (String target : targets) {
				if (target == null) {
					continue;
				}
				for (String tx : texts) {
					if (target.toLowerCase().contains(tx)) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean equalTo(String text) {
			if (text == null) {
				return false;
			}
			text = text.toLowerCase();
			String[] texts = text.split(" or ");
			for (String target : targets) {
				if (target == null) {
					continue;
				}
				for (String tx : texts) {
					if (target.equalsIgnoreCase(tx)) {
						return true;
					}
				}
			}
			return false;
		}

		public SearchTextMatcher targets(List<String> targetList) {
			targets = (String[]) targetList
					.toArray(new String[targetList.size()]);
			return this;
		}

		public SearchTextMatcher targets(String... targets) {
			this.targets = targets;
			return this;
		}
	}

	@Reflected
	@Registration(SearchUtilsIdsHelper.class)
	public static abstract class SearchUtilsIdsHelper {
		public static SearchUtils.SearchUtilsIdsHelper get() {
			return Registry.impl(SearchUtils.SearchUtilsIdsHelper.class);
		}

		public abstract Set<Long> getIds(String query);

		public boolean matches(String query, Entity entity) {
			return false;
		}
	}

	@Registration.Singleton(SearchUtilsIdsHelper.class)
	public static class SearchUtilsIdsHelperSingleThreaded
			extends SearchUtilsIdsHelper {
		private CachingMap<String, Set<Long>> stringIdLookup = new CachingMap<>(
				s -> s == null || !s.matches(IDS_REGEX) ? new LinkedHashSet<>()
						: idsTextToSet(s),
				getMap());

		@Override
		public Set<Long> getIds(String query) {
			Set<Long> ids = null;
			synchronized (stringIdLookup) {
				ids = stringIdLookup.get(query);
			}
			return ids;
		}

		protected Map<String, Set<Long>> getMap() {
			return new LinkedHashMap<>();
		}

		@Override
		public boolean matches(String query, Entity entity) {
			Set<Long> ids = getIds(query);
			return entity != null && ids.contains(entity.getId());
		}
	}

	@Reflected
	@Registration(SearchUtilsRegExpHelper.class)
	public static abstract class SearchUtilsRegExpHelper {
		public boolean matches(String query, Entity entity) {
			return false;
		}
	}

	@Registration.Singleton(SearchUtilsRegExpHelper.class)
	public static class SearchUtilsRegExpHelperSingleThreaded
			extends SearchUtilsRegExpHelper {
		private CachingMap<String, RegExp> stringRegexpLookup = new CachingMap<>(
				s -> s == null || !s.matches(REGEX_REGEX) ? null
						: RegExp.compile(s.replaceFirst(REGEX_REGEX, "public"),
								"i"),
				getMap());

		protected Map<String, RegExp> getMap() {
			return new LinkedHashMap<>();
		}

		@Override
		public boolean matches(String query, Entity entity) {
			if (entity == null) {
				return false;
			}
			RegExp regExp = null;
			synchronized (stringRegexpLookup) {
				regExp = stringRegexpLookup.get(query);
			}
			if (regExp == null) {
				return false;
			}
			return regExp.exec(entity.toString()) != null;
		}
	}

	public static LongestSubstringMatch getLongestSubstringMatch(String text,
			String match) {
		return new LongestSubstringMatch(text, match);
	}

	public static class LongestSubstringMatch {
		String text;

		String match;

		public int longestSubstringMatchIdx;

		public String longestSubstringMatch;

		public String subsequentToLongestSubstringMatch;

		LongestSubstringMatch(String text, String match) {
			this.text = text;
			this.match = match;
			for (int substringLength = match
					.length(); substringLength >= 0; substringLength--) {
				String test = match.substring(0, substringLength);
				int idx = text.indexOf(test);
				if (idx != -1) {
					longestSubstringMatchIdx = idx;
					longestSubstringMatch = test;
					subsequentToLongestSubstringMatch = text
							.substring(idx + test.length());
					break;
				}
			}
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("longestSubstringMatchIdx",
					longestSubstringMatchIdx, "longestSubstringMatch",
					longestSubstringMatch, "subsequentToLongestSubstringMatch",
					Ax.trim(subsequentToLongestSubstringMatch, 50));
		}
	}

	public static String unquoteAndTrim(String string) {
		string = Ax.ntrim(string);
		string = string.replaceFirst("^\"(.+)\"$", "$1");
		return string;
	}
}
