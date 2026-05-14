package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.List;
import java.util.Optional;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;

@Reflected
public enum StandardSearchOperator implements SearchOperator {
	CONTAINS, DOES_NOT_CONTAIN, EQUALS, DOES_NOT_EQUAL, LESS_THAN,
	LESS_THAN_OR_EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, ALL_OF,
	AT_LEAST_ONE_OF, STARTS_WITH;

	public static transient List<StandardSearchOperator> TEXT = List.of(
			CONTAINS, DOES_NOT_CONTAIN, EQUALS, DOES_NOT_EQUAL, STARTS_WITH,
			LESS_THAN, LESS_THAN_OR_EQUAL_TO, GREATER_THAN,
			GREATER_THAN_OR_EQUAL_TO);

	public static transient List LINEAR = List.of(EQUALS, LESS_THAN,
			GREATER_THAN);

	public static transient List EQUAL_OR_NOT = List.of(EQUALS, DOES_NOT_EQUAL);

	public static transient List EQUAL = List.of(EQUALS);

	public static transient List CONTAINS_AND_ALL_OF = List.of(CONTAINS,
			ALL_OF);

	public static transient List MEMBERSHIP = List.of(CONTAINS, ALL_OF,
			DOES_NOT_CONTAIN);

	private String displayName;

	private StandardSearchOperator() {
	}

	private StandardSearchOperator(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getName() {
		return Optional.ofNullable(displayName)
				.orElse(CommonUtils.friendlyConstant(this));
	}

	public boolean isOrdered() {
		switch (this) {
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUAL_TO:
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL_TO:
			return true;
		default:
			return false;
		}
	}

	public boolean evaluateComparatorResult(int comparatorResult) {
		switch (this) {
		case GREATER_THAN:
			return comparatorResult > 0;
		case GREATER_THAN_OR_EQUAL_TO:
			return comparatorResult >= 0;
		case LESS_THAN:
			return comparatorResult < 0;
		case LESS_THAN_OR_EQUAL_TO:
			return comparatorResult <= 0;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public FilterOperator toFilterOperator() {
		switch (this) {
		case ALL_OF:
		case AT_LEAST_ONE_OF:
		case DOES_NOT_CONTAIN:
		case STARTS_WITH:
			return null;
		case CONTAINS:
			return FilterOperator.CONTAINS;
		case DOES_NOT_EQUAL:
			return FilterOperator.NE;
		case EQUALS:
			return FilterOperator.EQ;
		case GREATER_THAN:
			return FilterOperator.GT;
		case GREATER_THAN_OR_EQUAL_TO:
			return FilterOperator.GT_EQ;
		case LESS_THAN:
			return FilterOperator.LT;
		case LESS_THAN_OR_EQUAL_TO:
			return FilterOperator.LT_EQ;
		default:
			throw new UnsupportedOperationException();
		}
	}
}
