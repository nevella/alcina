/**
 * 
 */
package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.totsp.gwittir.client.beans.Converter;

/**
 * A string tuple
 * 
 * @author nick@alcina.cc
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class StringPair implements Serializable {
	public static final Converter<StringPair, Object> SECOND_ITEM_CONVERTER = null;

	public String s1;

	public String s2;

	public StringPair() {
	}

	public StringPair(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringPair) {
			StringPair o = (StringPair) obj;
			return Objects.equals(s1, o.s1) && Objects.equals(s2, o.s2);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(s1, s2);
	}

	@Override
	public String toString() {
		return Ax.format("s1:%s\ns2:%s", s1, s2);
	}

	public static class StringPairFlattener
			implements Converter<StringPair, String> {
		private boolean second;

		public StringPairFlattener(boolean second) {
			this.second = second;
		}

		@Override
		public String convert(StringPair original) {
			return second ? original.s2 : original.s1;
		}
	}
}