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

	public int compareTops(Rect otherRect) {
		return (y1 < otherRect.y1 ? -1 : (y1 == otherRect.y1 ? 0 : 1));
	}

	public int getHeight() {
		return y2 - y1;
	}

	public int getWidth() {
		return x2 - x1;
	}

	public float overlapHorizontal(Rect other) {
		FloatPair fp1 = new FloatPair(x1, x2);
		FloatPair fp2 = new FloatPair(other.x1, other.x2);
		return fp1.overlap(fp2);
	}

	public float overlapVertical(Rect other) {
		FloatPair fp1 = new FloatPair(y1, y2);
		FloatPair fp2 = new FloatPair(other.y1, other.y2);
		return fp1.overlap(fp2);
	}

	@Override
	public String toString() {
		return Ax.format("(%s,%s)-(%s,%s)", x1, y1, x2, y2);
	}
}