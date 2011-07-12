/**
 * 
 */
package cc.alcina.extras.collections;

public class IntPair implements Comparable<IntPair> {
	public int i1;

	public int i2;

	public IntPair() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntPair) {
			IntPair ip = (IntPair) obj;
			return i1 == ip.i1 && i2 == ip.i2;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return i1 ^ i2;
	}

	public IntPair(int i1, int i2) {
		super();
		this.i1 = i1;
		this.i2 = i2;
	}

	public void add(IntPair ip) {
		i1 += ip.i1;
		i2 += ip.i2;
	}

	public void subtract(IntPair ip) {
		i1 -= ip.i1;
		i2 -= ip.i2;
	}

	public void max(IntPair ip) {
		i1 = i1 == 0 ? ip.i1 : Math.min(i1, ip.i1);
		i2 = i2 == 0 ? ip.i2 : Math.max(i1, ip.i2);
	}

	public void expand(int value) {
		i1 = i1 == 0 ? value : Math.min(i1, value);
		i2 = i2 == 0 ? value : Math.max(i2, value);
	}

	public int compareTo(IntPair ip) {
		return i1 < ip.i1 ? -1 : i1 > ip.i1 ? 1 : i2 < ip.i2 ? -1
				: i2 > ip.i2 ? 1 : 0;
	}

	@Override
	public String toString() {
		return "[" + i1 + "," + i2 + "]";
	}

	public boolean isZero() {
		return i1 == 0 && i2 == 0;
	}
	//as a possible vector in 1-space
	public boolean isPoint() {
		return i1 ==i2;
	}

	public IntPair intersection(IntPair other) {
		IntPair result=new IntPair(Math.max(i1,other.i1),Math.min(i2,other.i2));
		return result.i1<=result.i2?result:null;
	}
}