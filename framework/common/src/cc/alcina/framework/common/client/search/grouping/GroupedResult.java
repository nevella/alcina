package cc.alcina.framework.common.client.search.grouping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.util.AlcinaCollectors;

public class GroupedResult implements Serializable {
	public String name;

	public String json;

	public String html;

	private List<Row> rows = new ArrayList<>();

	private List<Col> cols = new ArrayList<>();

	private Row totalRow;

	public Stream<Cell> allCells() {
		return getRows().stream().map(row -> row.cells)
				.collect(AlcinaCollectors.toItemStream());
	}

	public List<Col> getCols() {
		return this.cols;
	}

	public int getColumnIndex(String columnName) {
		Col col = cols.stream().filter(c -> c.name.equals(columnName))
				.findFirst().orElse(null);
		return cols.indexOf(col);
	}

	public List<Row> getRows() {
		return this.rows;
	}

	public List<Row> getRowsNoTotal() {
		return getRows().stream().filter(l -> l != getTotalRow())
				.collect(Collectors.toList());
	}

	public Row getTotalRow() {
		return totalRow;
	}

	public double getTotalValue(String columnName) {
		int idx = getColumnIndex(columnName);
		return totalRow.cells.get(idx).numericValue;
	}

	public List provideModelLines() {
		return getRows().stream().map(row -> row.in)
				.collect(Collectors.toList());
	}

	public void setCols(List<Col> cols) {
		this.cols = cols;
	}

	public void setRows(List<Row> rows) {
		this.rows = rows;
	}

	public void setTotalRow(Row totalRow) {
		this.totalRow = totalRow;
	}

	public List<String> toHeaderList() {
		return cols.stream().map(col -> col.name).collect(Collectors.toList());
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Cell implements Serializable {
		public transient Object rawValue;

		public String value;

		public String href;

		public String target;

		public String title;

		public Double numericValue;

		public Cell() {
		}
	}

	public static class Col implements Serializable {
		public String name;

		public String style;

		public double width;

		@XmlTransient
		public Unit unit;

		public GroupKey key;

		public String color;

		@GwtTransient
		public boolean numeric;

		public Col() {
		}

		public Col withColor(String color) {
			this.color = color;
			return this;
		}

		public Col withGroupKey(GroupKey key) {
			this.key = key;
			return this;
		}

		public Col withName(String name) {
			this.name = name;
			return this;
		}

		public Col withNumeric(boolean numeric) {
			this.numeric = numeric;
			return this;
		}

		public Col withStyle(String style) {
			this.style = style;
			return this;
		}

		public Col withWidth(double width, Unit unit) {
			this.width = width;
			this.unit = unit;
			return this;
		}
	}

	public static class GroupKey implements Serializable {
		public GroupKey() {
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Row implements Serializable {
		public transient Object in;

		public GroupKey key;

		public String section;

		public List<Cell> cells = new ArrayList<>();

		public Row() {
		}

		public Row(Object intermediateObject) {
			this.in = intermediateObject;
		}

		public List<String> toStringList() {
			return cells.stream().map(cell -> cell.value)
					.collect(Collectors.toList());
		}
	}

	public static class VoidGroupedResultRow extends Row {
	}
}
