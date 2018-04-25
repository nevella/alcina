package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Date;
import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.datepicker.client.CalendarUtil;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.common.client.search.TxtCriterion.TxtCriterionType;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;

public class SearchUtils {
//	private static CachingMap<String, Long> stringIdLookup = new CachingMap<>(
//			s -> s == null || !s.matches("(?:id:)?[0-9]+") ? 0L
//					: Long.parseLong(s.replaceFirst("(?:id:)?([0-9]+)", "$1")));

	public static boolean containsIgnoreCase(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return false;
		}
		String lc1 = s1.toLowerCase();
		String lc2 = s2.toLowerCase();
		return lc1.contains(lc2);
	}

	public static boolean containsIgnoreCase(String text, String... strings) {
		return new SearchTextMatcher().targets(strings).contains(text);
	}

	public static boolean containsIgnoreCase(String text,
			List<String> strings) {
		return new SearchTextMatcher().targets(strings).contains(text);
	}

	public static boolean equalsIgnoreCase(String text, String... strings) {
		return new SearchTextMatcher().targets(strings).equalTo(text);
	}

	public static boolean equalsIgnoreCase(String text, List<String> strings) {
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

	public static boolean matchesId(String query, HasIdAndLocalId hili) {
		return hili != null && toId(query) == hili.getId();
	}

	public static long toId(String s) {
		return s == null || !s.matches("(?:id:)?[0-9]+") ? -1L
				: Long.parseLong(s.replaceFirst("(?:id:)?([0-9]+)", "$1"));
		// return stringIdLookup.get(text);
	}

	public static void toTextSearch(SearchDefinition def, String text) {
		RegExp regExp = RegExp.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})", "g");
		MatchResult result;
		Date date = null;
		if ((result = regExp.exec(text)) != null) {
			date = CommonUtils.yearAsDate(Integer.parseInt(result.getGroup(3)));
			CalendarUtil.addMonthsToDate(date,
					Integer.parseInt(result.getGroup(2)) - 1);
			CalendarUtil.addDaysToDate(date,
					Integer.parseInt(result.getGroup(1)) - 1);
		}
		if (date != null) {
			for (SearchCriterion criterion : def.allCriteria()) {
				if (criterion instanceof DateCriterion) {
					if (criterion.getDirection() == Direction.ASCENDING) {
						Date d2 = new Date(date.getTime());
						CalendarUtil.addDaysToDate(d2, -2);
						((DateCriterion) criterion).setDate(d2);
					} else {
						Date d2 = new Date(date.getTime());
						CalendarUtil.addDaysToDate(d2, +2);
						((DateCriterion) criterion).setDate(d2);
					}
				}
			}
		} else {
			TxtCriterion txtCriterion = new TxtCriterion();
			txtCriterion.setText(text);
			txtCriterion.setTxtCriterionType(TxtCriterionType.CONTAINS);
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

		public SearchTextMatcher targets(List<String> targetList) {
			targets = (String[]) targetList
					.toArray(new String[targetList.size()]);
			return this;
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

		public SearchTextMatcher targets(String... targets) {
			this.targets = targets;
			return this;
		}
	}
}
