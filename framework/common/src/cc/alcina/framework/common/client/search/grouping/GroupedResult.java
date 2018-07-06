package cc.alcina.framework.common.client.search.grouping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

public class GroupedResult<IN> implements Serializable {
	public String name;

	private List<Row<IN>> rows = new ArrayList<>();

	private List<Col> cols = new ArrayList<>();

	private Row<IN> totalRow;

	public List<Col> getCols() {
		return this.cols;
	}

	public List<Row<IN>> getRows() {
		return this.rows;
	}

	public List<Row<IN>> getRowsNoTotal() {
		return getRows().stream().filter(l -> l != getTotalRow())
				.collect(Collectors.toList());
	}

	public Row getTotalRow() {
		return totalRow;
	}

	public List<IN> provideModelLines() {
		return getRows().stream().map(row -> row.in)
				.collect(Collectors.toList());
	}

	public void setCols(List<Col> cols) {
		this.cols = cols;
	}

	public void setRows(List<Row<IN>> rows) {
		this.rows = rows;
	}

	public void setTotalRow(Row<IN> totalRow) {
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

		public Cell() {
		}
	}

	public static class Col implements Serializable {
		public String name;

		public String style;

		public String width;

		public Col() {
		}

		public Col withName(String name) {
			this.name = name;
			return this;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Row<IN> implements Serializable {
		public transient IN in;

		public RowKey key;

		public String section;

		public List<Cell> cells = new ArrayList<>();

		public Row() {
		}

		public Row(IN intermediateObject) {
			this.in = intermediateObject;
		}

		public List<String> toStringList() {
			return cells.stream().map(cell -> cell.value)
					.collect(Collectors.toList());
		}
	}

	public static class RowKey implements Serializable {
		public RowKey() {
		}
	}

	public static class VoidGroupedResultRow extends Row {
	}
}
