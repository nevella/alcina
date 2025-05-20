package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;

@Reflected
public enum StandardSearchOperator implements SearchOperator {
	CONTAINS, DOES_NOT_CONTAIN, EQUALS, LESS_THAN, GREATER_THAN, ALL_OF,
	AT_LEAST_ONE_OF, DOES_NOT_EQUAL, STARTS_WITH, LESS_THAN_OR_EQUAL_TO,
	GREATER_THAN_OR_EQUAL_TO;

	public static transient List LINEAR = Arrays.asList(EQUALS, LESS_THAN,
			GREATER_THAN);

	public static transient List EQUAL_OR_NOT = Arrays.asList(EQUALS,
			DOES_NOT_EQUAL);

	public static transient List EQUAL = Arrays.asList(EQUALS);

	public static transient List CONTAINS_AND_ALL_OF = Arrays.asList(CONTAINS,
			ALL_OF);

	public static transient List MEMBERSHIP = Arrays.asList(CONTAINS, ALL_OF,
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
}
