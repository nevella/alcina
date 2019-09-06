package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.entity.projection.GraphProjection;

public class SimpleAtomModel {
	@XmlAccessorType(XmlAccessType.FIELD)
	public static abstract class AtomCell<CK, T, AC extends AtomCell> {
		public String name;

		public CK cellKey;

		protected transient List<T> cellMembers = new ArrayList<>();

		public AtomCell() {
		}

		public AtomCell(CK cellKey) {
			this.cellKey = cellKey;
			this.name = cellName();
		}

		public void addMember(T member) {
			cellMembers.add(member);
		}

		protected abstract void accumulate(AC cell);

		protected String cellName() {
			return cellKey instanceof HasDisplayName
					? ((HasDisplayName) cellKey).displayName()
					: Ax.friendly(cellKey);
		}

		protected abstract void generateValues();

		protected void postConstructor() {
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static abstract class AtomKey {
		private transient int hash = 0;

		@Override
		public boolean equals(Object obj) {
			return GraphProjection.nonTransientFieldwiseEqual(this, obj);
		}

		@Override
		public int hashCode() {
			if (hash == 0) {
				hash = GraphProjection.nonTransientFieldwiseHash(this);
				if (hash == 0) {
					hash = -1;
				}
			}
			return hash;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToString(this);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static abstract class AtomLine<K extends AtomKey, CK, T, C extends AtomCell<CK, T, C>> {
		public K key;

		public List<C> cells = new ArrayList<>();

		public C rowTotal;

		public transient Map<CK, C> cellsByKey = new LinkedHashMap<>();

		public AtomLine() {
			ensureCells();
		}

		public AtomLine(K key) {
			this.key = key;
			ensureCells();
		}

		public void addCell(C cell) {
			cell.postConstructor();
			cells.add(cell);
			cellsByKey.put(cell.cellKey, cell);
		}

		public void generateRowTotal() {
			if (cells.size() == 0) {
				return;
			}
			try {
				C cell = (C) cells.get(0).getClass().newInstance();
				cells.forEach(cell::accumulate);
				rowTotal = cell;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public void generateValues() {
			cells.forEach(AtomCell::generateValues);
		}

		public C getCell(CK cellKey) {
			return cellsByKey.get(cellKey);
		}

		protected void ensureCells() {
		}
	}

	// = new CachingMap<K, L>(
	// L::new);
	@XmlAccessorType(XmlAccessType.FIELD)
	public static abstract class AtomModel<K extends AtomKey, L extends AtomLine> {
		public List<L> lines = new ArrayList<>();

		public L totalLine;

		public transient CachingMap<K, L> intermediate;

		public void generateRowTotals() {
			lines.forEach(AtomLine::generateRowTotal);
		}

		public void generateTotalRow(boolean withRowTotals) {
			try {
				if (lines.isEmpty()) {
					return;
				}
				L first = lines.get(0);
				this.totalLine = (L) first.getClass().newInstance();
				for (AtomCell cell : (List<AtomCell>) totalLine.cells) {
					for (L line : lines) {
						for (AtomCell cell2 : (List<AtomCell>) line.cells) {
							if (cell.cellKey.equals(cell2.cellKey)) {
								cell.accumulate(cell2);
							}
						}
					}
				}
				if (withRowTotals) {
					totalLine.generateRowTotal();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public void toList() {
			lines = intermediate.values().stream().sorted()
					.collect(Collectors.toList());
			lines.forEach(AtomLine::generateValues);
		}
	}
}
