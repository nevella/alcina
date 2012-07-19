package cc.alcina.framework.common.client.util;


public class Rect {
	public int x1;

	public int x2;

	public int y1;

	public int y2;

	public Rect(int x1, int x2, int y1, int y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("(%s,%s)-(%s,%s)", x1, y1, x2, y2);
	}

	public int compareTops(Rect otherRect) {
		return (y1 < otherRect.y1 ? -1 : (y1 == otherRect.y1 ? 0 : 1));
	}

	public int getHeight() {
		return y2 - y1;
	}

	public int getWidth() {
		return x2 - x1;
	}
}