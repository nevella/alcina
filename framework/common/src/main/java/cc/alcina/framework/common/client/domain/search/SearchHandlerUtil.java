package cc.alcina.framework.common.client.domain.search;

import java.util.Date;
import java.util.Set;

import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.StandardSearchOperator;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.util.DateUtil;

public class SearchHandlerUtil {
	public static boolean allowDate(DateCriterion sc, Date date,
			boolean nullAsOk) {
		return allowDate(sc, date, nullAsOk, null, true);
	}

	public static boolean allowDate(DateCriterion sc, Date date,
			boolean nullAsOk, StandardSearchOperator op, boolean roundDate) {
		if (date == null) {
			return nullAsOk;
		}
		Date criterionDate = sc.getValue();
		if (roundDate) {
			criterionDate = new Date(sc.getValue().getTime());
			DateUtil.roundDate(criterionDate,
					sc.getDirection() == Direction.DESCENDING);
		}
		boolean eq = criterionDate.equals(date);
		boolean lt = date.before(criterionDate);
		boolean gt = date.after(criterionDate);
		if (op == null) {
			op = sc.rangeControlledByDirection()
					? sc.getDirection() == Direction.ASCENDING
							? StandardSearchOperator.GREATER_THAN
							: StandardSearchOperator.LESS_THAN
					: sc.getOperator();
		}
		switch (op) {
		case EQUALS:
			return eq;
		case DOES_NOT_EQUAL:
			return !eq;
		case GREATER_THAN:
			return gt;
		case LESS_THAN:
			return lt;
		case GREATER_THAN_OR_EQUAL_TO:
			return gt || eq;
		case LESS_THAN_OR_EQUAL_TO:
			return lt || eq;
		default:
			throw new UnsupportedOperationException();
		}
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
