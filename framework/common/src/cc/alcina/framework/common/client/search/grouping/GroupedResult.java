package cc.alcina.framework.common.client.search.grouping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gwt.dom.client.Style.Unit;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class GroupedResult extends Model {
	private String name;

	private String json;

	private String html;

	private List<Row> rows = new ArrayList<>();

	private List<Col> cols = new ArrayList<>();

	private Row totalRow;

	public Stream<Cell> allCells() {
		return getRows().stream().map(row -> row.getCells())
				.collect(AlcinaCollectors.toItemStream());
	}

	public List<Col> getCols() {
		return this.cols;
	}

	public int getColumnIndex(String columnName) {
		Col col = cols.stream().filter(c -> c.getName().equals(columnName))
				.findFirst().orElse(null);
		return cols.indexOf(col);
	}

	public String getHtml() {
		return html;
	}

	public String getJson() {
		return json;
	}

	public String getName() {
		return name;
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
		return totalRow.getCells().get(idx).getNumericValue();
	}

	public List provideModelLines() {
		return getRows().stream().map(row -> row.in)
				.collect(Collectors.toList());
	}

	public void setCols(List<Col> cols) {
		this.cols = cols;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRows(List<Row> rows) {
		this.rows = rows;
	}

	public void setTotalRow(Row totalRow) {
		this.totalRow = totalRow;
	}

	public List<String> toHeaderList() {
		return cols.stream().map(col -> col.getName())
				.collect(Collectors.toList());
	}

	public static class Cell extends Model {
		public transient Object rawValue;

		private String value;

		private String href;

		private String target;

		private String title;

		private Double numericValue;

		public Cell() {
		}

		public String getHref() {
			return href;
		}

		public Double getNumericValue() {
			return numericValue;
		}

		public String getTarget() {
			return target;
		}

		public String getTitle() {
			return title;
		}

		public String getValue() {
			return value;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public void setNumericValue(Double numericValue) {
			this.numericValue = numericValue;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class Col extends Model {
		private String name;

		private String style;

		private double width;

		private Unit unit;

		private GroupKey key;

		private String color;

		private boolean numeric;

		public Col() {
		}

		public String getColor() {
			return color;
		}

		public GroupKey getKey() {
			return key;
		}

		public String getName() {
			return name;
		}

		public String getStyle() {
			return style;
		}

		@XmlTransient
		public Unit getUnit() {
			return unit;
		}

		public double getWidth() {
			return width;
		}

		public boolean isNumeric() {
			return numeric;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public void setKey(GroupKey key) {
			this.key = key;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setNumeric(boolean numeric) {
			this.numeric = numeric;
		}

		public void setStyle(String style) {
			this.style = style;
		}

		public void setUnit(Unit unit) {
			this.unit = unit;
		}

		public void setWidth(double width) {
			this.width = width;
		}

		public Col withColor(String color) {
			this.setColor(color);
			return this;
		}

		public Col withGroupKey(GroupKey key) {
			this.setKey(key);
			return this;
		}

		public Col withName(String name) {
			this.setName(name);
			return this;
		}

		public Col withNumeric(boolean numeric) {
			this.setNumeric(numeric);
			return this;
		}

		public Col withStyle(String style) {
			this.setStyle(style);
			return this;
		}

		public Col withWidth(double width, Unit unit) {
			this.setWidth(width);
			this.setUnit(unit);
			return this;
		}
	}

	public static class GroupKey extends Model {
		public GroupKey() {
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Row extends Model {
		public transient Object in;

		private GroupKey key;

		private String section;

		private List<Cell> cells = new ArrayList<>();

		public Row() {
		}

		public Row(Object intermediateObject) {
			this.in = intermediateObject;
		}

		public List<Cell> getCells() {
			return cells;
		}

		public GroupKey getKey() {
			return key;
		}

		public String getSection() {
			return section;
		}

		public List<Cell> setCells(List<Cell> cells) {
			this.cells = cells;
			return cells;
		}

		public GroupKey setKey(GroupKey key) {
			this.key = key;
			return key;
		}

		public void setSection(String section) {
			this.section = section;
		}

		public List<String> toStringList() {
			return getCells().stream().map(cell -> cell.getValue())
					.collect(Collectors.toList());
		}
	}

	public static class VoidGroupedResultRow extends Row {
	}
}
