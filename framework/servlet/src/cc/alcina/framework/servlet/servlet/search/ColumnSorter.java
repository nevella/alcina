package cc.alcina.framework.servlet.servlet.search;

import java.util.Comparator;
import java.util.List;

import cc.alcina.framework.common.client.domain.search.SearchOrders.ColumnSearchOrder;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Col;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ColumnSorter implements Comparator<Row> {
	private List<ColumnSearchOrder> columnOrders;

	private GroupedResult groupedResult;

	CachingMap<String, Double> numericLookup = new CachingMap<>(
			this::parseNumeric);

	public ColumnSorter(List<ColumnSearchOrder> columnOrders,
			GroupedResult groupedResult) {
		this.columnOrders = columnOrders;
		this.groupedResult = groupedResult;
	}

	@Override
	public int compare(Row o1, Row o2) {
		for (ColumnSearchOrder order : columnOrders) {
			int idx = groupedResult.getColumnIndex(order.getColumnName());
			Col col = groupedResult.getCols().get(idx);
			boolean numeric = col.numeric
					|| col.name.equalsIgnoreCase("numeric");
			String v1 = o1.cells.get(idx).value;
			String v2 = o2.cells.get(idx).value;
			int i = 0;
			if (numeric) {
				i = numericLookup.get(v1).compareTo(numericLookup.get(v2));
			} else {
				i = CommonUtils.compareWithNullMinusOne(v1, v2);
			}
			i = i * (order.isAscending() ? 1 : -1);
			if (i != 0) {
				return i;
			}
		}
		return 0;
	}

	Double parseNumeric(String s) {
		if (s == null) {
			return 0.0;
		}
		String cleaned = s.replaceAll("[^0-9.\\-]", "");
		if (cleaned.length() > 0) {
			try {
				return Double.parseDouble(cleaned);
			} catch (Exception e) {
				return 0.0;
			}
		} else {
			return 0.0;
		}
	}
}
