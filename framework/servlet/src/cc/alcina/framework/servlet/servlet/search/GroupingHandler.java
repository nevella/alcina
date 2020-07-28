package cc.alcina.framework.servlet.servlet.search;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.gwt.client.entity.search.GroupingParameters;

public interface GroupingHandler<DS extends VersionableEntity, GP extends GroupingParameters> {
	GroupedResult process(List<DS> queried, GP groupingParameters,
			SearchDefinition def);

	default void sort(GroupedResult groupedResult,
			GroupingParameters groupingParameters) {
		if (groupingParameters.getColumnOrders().size() > 0) {
			List<Row> rows = groupedResult.getRowsNoTotal();
			Collections.sort(rows, new ColumnSorter(
					groupingParameters.getColumnOrders(), groupedResult));
			if (groupedResult.getTotalRow() != null) {
				rows.add(groupedResult.getTotalRow());
			}
			groupedResult.setRows(rows);
		}
	}

	default String suggestFileName(String prefix, GP groupingParameters) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
		return String.format("%s-%s", prefix, df.format(new Date()));
	}
}
