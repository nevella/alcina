package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@Reflected
@Bean(PropertySource.FIELDS)
public class DatePair
		implements Comparable<DatePair>, Serializable, TreeSerializable {
	public Date d1;

	public Date d2;

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

	public boolean contains(Date date) {
		return d1.compareTo(date) <= 0 && d2.compareTo(date) >= 0;
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

	public String toHumanDateRange() {
		return Ax.format("%s to %s", Ax.dateSlash(d1), Ax.dateSlash(d2));
	}

	@Override
	public String toString() {
		return Ax.format("[%s -> %s]", d1, d2);
	}
}
