package cc.alcina.framework.common.client.util;

import java.util.Objects;

/**
 * This class is intended for use as a two-valued map key
 */
public class Pair {
	Object o1;

	Object o2;

	int hash;

	Pair(Object o1, Object o2) {
		this.o1 = o1;
		this.o2 = o2;
		this.hash = Objects.hash(o1, o2);
	}

	@Override
	public int hashCode() {
		return hash;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Pair){
			Pair o = (Pair) obj;
			return Objects.equals(o1, o.o1)&&Objects.equals(o2, o.o2);
		}else{
			return false;
		}
	}

	public static Pair of(Object o1, Object o2) {
		return new Pair(o1, o2);
	}
}
