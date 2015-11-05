package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.Optional;

import cc.alcina.framework.common.client.util.CommonUtils;

public enum StandardSearchOperator implements SearchOperator {
	CONTAINS, DOES_NOT_CONTAIN, EQUALS, LESS_THAN, GREATER_THAN;
	private String displayName;

	private StandardSearchOperator() {
	}

	private StandardSearchOperator(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getName() {
		return Optional.ofNullable(displayName).orElse(
				CommonUtils.friendlyConstant(this));
	}
}
