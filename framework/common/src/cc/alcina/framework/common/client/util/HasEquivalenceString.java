package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;

import com.totsp.gwittir.client.beans.Converter;

public interface HasEquivalenceString<T> extends HasEquivalenceHash<T> {
	public String equivalenceString();

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface HasEquivalenceInfo {
		String value();
	}

	default public int equivalenceHash() {
		return equivalenceString().hashCode();
	}

	default public boolean equivalentTo(T other) {
		return equivalenceString().equals(
				((HasEquivalenceString<T>) other).equivalenceString());
	}

	public static final class HasEquivalenceStringConverter implements
			Converter<HasEquivalenceString, String> {
		@Override
		public String convert(HasEquivalenceString original) {
			return original.equivalenceString();
		}
	}
}
