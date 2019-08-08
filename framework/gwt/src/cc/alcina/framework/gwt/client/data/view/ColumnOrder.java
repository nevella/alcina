package cc.alcina.framework.gwt.client.data.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder.SortableColumn;
import cc.alcina.framework.gwt.client.cell.DirectedComparator;

//non-typed, to avoid gwt type checks on native comparators
public class ColumnOrder implements Comparator {
	List<Comparator> cmps = new ArrayList<>();

	public ColumnOrder(ColumnSortList columnSortList) {
		for (int idx = 0; idx < columnSortList.size(); idx++) {
			ColumnSortInfo columnSortInfo = columnSortList.get(idx);
			SortableColumn column = (SortableColumn) columnSortInfo.getColumn();
			Function<Object, Comparable> sortFunction = column.sortFunction();
			DirectedComparator nativeComparator = ((SortableColumn) columnSortInfo
					.getColumn()).getNativeComparator();
			if (GWT.isScript() && nativeComparator != null) {
				nativeComparator.direction = columnSortInfo.isAscending() ? 1
						: -1;
				cmps.add(nativeComparator);
			} else {
				cmps.add(new Comparator() {
					@Override
					public int compare(Object o1, Object o2) {
						int dir = columnSortInfo.isAscending() ? 1 : -1;
						return dir * CommonUtils.compareWithNullMinusOne(
								sortFunction.apply(o1), sortFunction.apply(o2));
					}
				});
			}
		}
	}

	@Override
	public int compare(Object o1, Object o2) {
		for (int idx = 0; idx < cmps.size(); idx++) {
			Comparator cmp = cmps.get(idx);
			int i = cmp.compare(o1, o2);
			if (i != 0) {
				return i;
			}
		}
		return 0;
	}
}