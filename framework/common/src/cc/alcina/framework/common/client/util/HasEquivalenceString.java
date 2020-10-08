package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.stream.Stream;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;

public interface HasEquivalenceString<T> extends HasEquivalenceHash<T> {
	@Override
	default public int equivalenceHash() {
		return equivalenceString().hashCode();
	}

	public String equivalenceString();

	@Override
	default public boolean equivalentTo(T other) {
		return equivalenceString()
				.equals(((HasEquivalenceString<T>) other).equivalenceString());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface HasEquivalenceInfo {
		String value();
	}

	public static final class HasEquivalenceStringConverter
			implements Converter<HasEquivalenceString, String> {
		@Override
		public String convert(HasEquivalenceString original) {
			return original.equivalenceString();
		}
	}

	public static <T extends HasEquivalenceString> Map<String, T>
			toEquivalanceStringMap(Stream<T> stream) {
		return stream.collect(AlcinaCollectors
				.toKeyMap(HasEquivalenceString::equivalenceString));
	}
}
