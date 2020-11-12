package cc.alcina.framework.servlet.google;

import cc.alcina.framework.common.client.util.Ax;

public abstract class SheetRow {
	private int rowIdx;

	private String sheetName;

	public int getRowIdx() {
		return this.rowIdx;
	}

	public String getSheetName() {
		return this.sheetName;
	}

	public void setRowIdx(int rowIdx) {
		this.rowIdx = rowIdx;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	public String toLocation() {
		return Ax.format("%s::%s", sheetName, rowIdx + 1);
	}
}
