/**
 */
package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)

		
		
@ClientInstantiable
@XmlAccessorType(XmlAccessType.FIELD)
@Registrations({ @Registration(JaxbContextRegistration.class),
		 })
public class LongPair
		implements Comparable<LongPair>, Predicate<Long>, TreeSerializable {
	public static boolean isContinuous(List<LongPair> matchedRanges) {
		LongPair union = unionOf(matchedRanges);
		return provideUncovered(matchedRanges, union).isEmpty();
	}

	public static LongPair parseLongPair(String string) {
		try {
			String[] split = string.replaceAll("[\\[\\] ]", "").split(",");
			if (split.length == 2) {
				return new LongPair(Long.parseLong(split[0]),
						Long.parseLong(split[1]));
			}
			long point = Long.parseLong(string);
			return new LongPair(point, point);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	public static List<LongPair> provideUncovered(List<LongPair> covered,
			LongPair container) {
		List<LongPair> result = new ArrayList<LongPair>();
		for (int idx = 0; idx <= covered.size(); idx++) {
			long from = idx == 0 ? container.l1 : covered.get(idx - 1).l2;
			long to = idx == covered.size() ? container.l2
					: covered.get(idx).l1;
			if (from != to) {
				result.add(new LongPair(from, to));
			}
		}
		return result;
	}

	public static LongPair unionOf(List<LongPair> matchedRanges) {
		// i.e clone
		LongPair result = matchedRanges.get(0).shiftRight(0);
		matchedRanges.forEach(ip -> {
			result.l1 = Math.min(ip.l1, result.l1);
			result.l2 = Math.max(ip.l2, result.l2);
		});
		return result;
	}

	public long l1;

	public long l2;

	public LongPair() {
	}

	public LongPair(long l1, long l2) {
		super();
		this.l1 = l1;
		this.l2 = l2;
	}

	public void add(LongPair ip) {
		l1 += ip.l1;
		l2 += ip.l2;
	}

	@Override
	public boolean test(Long o) {
		return o != null && o >= l1 && o < l2;
	}

	@Override
	public int compareTo(LongPair ip) {
		return l1 < ip.l1 ? -1
				: l1 > ip.l1 ? 1 : l2 < ip.l2 ? -1 : l2 > ip.l2 ? 1 : 0;
	}

	public boolean contains(long l) {
		return l >= l1 && l <= l2;
	}

	public boolean contains(LongPair other) {
		return other == null ? false : other.equals(intersection(other));
	}

	public boolean containsExBoundaries(LongPair other) {
		return contains(other) && l1 < other.l1 && l2 > other.l2;
	}

	public boolean containsIncludingBoundaries(Long o) {
		return o != null && o >= l1 && o <= l2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LongPair) {
			LongPair ip = (LongPair) obj;
			return l1 == ip.l1 && l2 == ip.l2;
		}
		return false;
	}

	public void expand(long value) {
		l1 = l1 == 0 ? value : Math.min(l1, value);
		l2 = l2 == 0 ? value : Math.max(l2, value);
	}

	public long getL1() {
		return this.l1;
	}

	public long getL2() {
		return this.l2;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(l1).hashCode() ^ Long.valueOf(l2).hashCode();
	}

	public LongPair intersection(LongPair other) {
		LongPair result = new LongPair(Math.max(l1, other.l1),
				Math.min(l2, other.l2));
		return result.l1 <= result.l2 ? result : null;
	}

	// as a possible vector in 1-space
	public boolean isPoint() {
		return l1 == l2;
	}

	public boolean isZero() {
		return l1 == 0 && l2 == 0;
	}

	public long length() {
		return l2 - l1;
	}

	public void max(LongPair ip) {
		l1 = l1 == 0 ? ip.l1 : Math.min(l1, ip.l1);
		l2 = l2 == 0 ? ip.l2 : Math.max(l1, ip.l2);
	}

	public void setL1(long l1) {
		this.l1 = l1;
	}

	public void setL2(long l2) {
		this.l2 = l2;
	}

	public LongPair shiftRight(int offset) {
		return new LongPair(l1 + offset, l2 + offset);
	}

	public void subtract(LongPair ip) {
		l1 -= ip.l1;
		l2 -= ip.l2;
	}

	@Override
	public String toString() {
		return "[" + l1 + "," + l2 + "]";
	}

	public static class LongPairFromStringConverter
			implements Converter<String, LongPair> {
		@Override
		public LongPair convert(String original) {
			return parseLongPair(original);
		}
	}
}
