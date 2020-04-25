package cc.alcina.framework.gwt.client.entity.search;

import java.util.Date;
import java.util.Set;

import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class SearchHandlerUtil {
	public static boolean allowDate(DateCriterion sc, Date date,
			boolean nullAsOk) {
		if (date == null) {
			return nullAsOk;
		}
		Date criterionDate = new Date(sc.getDate().getTime());
		CommonUtils.roundDate(criterionDate,
				sc.getDirection() == Direction.DESCENDING);
		boolean eq = criterionDate.equals(date);
		boolean lt = date.before(criterionDate);
		boolean gt = date.after(criterionDate);
		StandardSearchOperator op = sc.rangeControlledByDirection()
				? sc.getDirection() == Direction.ASCENDING
						? StandardSearchOperator.GREATER_THAN
						: StandardSearchOperator.LESS_THAN
				: sc.getOperator();
		if (eq) {
			return true;
		}
		switch (op) {
		case EQUALS:
			return false;
		case GREATER_THAN:
			return gt;
		case LESS_THAN:
			return lt;
		}
		return false;
	}

	public static <V> boolean matches(StandardSearchOperator operator,
			Set<V> test, Set<V> value) {
		switch (operator) {
		case ALL_OF:
			return value.containsAll(test);
		case AT_LEAST_ONE_OF:
		case CONTAINS:
			return value.stream().anyMatch(s -> test.contains(s));
		case DOES_NOT_CONTAIN:
			return !value.stream().anyMatch(s -> test.contains(s));
		case EQUALS:
			return value.containsAll(test) && value.size() == test.size();
		case DOES_NOT_EQUAL:
			return !(value.containsAll(test) && value.size() == test.size());
		}
		return false;
	}
}
