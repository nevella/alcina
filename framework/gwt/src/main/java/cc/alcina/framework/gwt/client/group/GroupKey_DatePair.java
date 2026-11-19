package cc.alcina.framework.gwt.client.group;

import cc.alcina.framework.common.client.search.grouping.GroupedResult.GroupKey;
import cc.alcina.framework.common.client.util.DatePair;

public class GroupKey_DatePair extends GroupKey {
	public DatePair datePair;

	public GroupKey_DatePair() {
	}

	public GroupKey_DatePair(DatePair datePair) {
		this.datePair = datePair;
	}
}