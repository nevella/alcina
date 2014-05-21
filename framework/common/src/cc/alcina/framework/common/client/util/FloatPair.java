/**
 * 
 */
package cc.alcina.framework.common.client.util;

public class FloatPair implements Comparable<FloatPair> {
	public float f1;

	public float f2;

	public FloatPair() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FloatPair) {
			FloatPair fp = (FloatPair) obj;
			return f1 == fp.f1 && f2 == fp.f2;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new Float(f1).hashCode() ^ new Float(f2).hashCode();
	}

	public FloatPair(float i1, float i2) {
		super();
		if (i1 <= i2) {
			this.f1 = i1;
			this.f2 = i2;
		} else {
			this.f1 = i2;
			this.f2 = i1;
		}
	}

	public void add(FloatPair fp) {
		f1 += fp.f1;
		f2 += fp.f2;
	}

	public void subtract(FloatPair fp) {
		f1 -= fp.f1;
		f2 -= fp.f2;
	}

	public void max(FloatPair fp) {
		f1 = f1 == 0 ? fp.f1 : Math.min(f1, fp.f1);
		f2 = f2 == 0 ? fp.f2 : Math.max(f1, fp.f2);
	}

	public void expand(float value) {
		f1 = f1 == 0 ? value : Math.min(f1, value);
		f2 = f2 == 0 ? value : Math.max(f2, value);
	}

	public int compareTo(FloatPair fp) {
		return f1 < fp.f1 ? -1 : f1 > fp.f1 ? 1 : f2 < fp.f2 ? -1
				: f2 > fp.f2 ? 1 : 0;
	}

	@Override
	public String toString() {
		return "[" + f1 + "," + f2 + "]";
	}

	public boolean isZero() {
		return f1 == 0 && f2 == 0;
	}

	// top exclusive
	public boolean contains(float f) {
		return f1 == f2 ? f1 == f : f1 <= f && f2 > f;
	}

	public float average() {
		return (f1 + f2) / 2;
	}

	public FloatPair intersection(FloatPair other) {
		FloatPair result = new FloatPair(Math.max(f1, other.f1), Math.min(f2,
				other.f2));
		return result.f1 <= result.f2 ? result : null;
	}

	public boolean intersectsWith(FloatPair other) {
		return intersection(other) != null;
	}
}