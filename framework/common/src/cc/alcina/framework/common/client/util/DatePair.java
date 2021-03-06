package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class DatePair implements Comparable<DatePair>, Serializable {
	public Date d1 = new Date();

	public Date d2 = new Date();

	public DatePair() {
	}

	public DatePair(Date d1, Date d2) {
		this.d1 = d1;
		this.d2 = d2;
	}

	@Override
	public int compareTo(DatePair o) {
		int i = d1.compareTo(o.d1);
		if (i == 0) {
			i = d2.compareTo(o.d2);
		}
		return i;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DatePair) {
			DatePair o = (DatePair) obj;
			return Objects.equals(d1, o.d1) && Objects.equals(d2, o.d2);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(d1, d2);
	}

	@Override
	public String toString() {
		return Ax.format("[%s -> %s]", d1, d2);
	}
}
