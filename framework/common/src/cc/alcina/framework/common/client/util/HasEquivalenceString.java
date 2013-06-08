package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import com.totsp.gwittir.client.beans.Converter;

public interface HasEquivalenceString {
	public String equivalenceString();

	public static class HasEquivalenceStringHelper {
		public static String collectionEquivalence(
				Collection<? extends HasEquivalenceString> collection) {
			if(collection==null){
				collection=new ArrayList<HasEquivalenceString>();
			}
			StringBuilder sb = new StringBuilder();
			for (HasEquivalenceString item : collection) {
				sb.append(item.equivalenceString());
				sb.append("\n");
			}
			return sb.toString();
		}

		public static String nullSafeEqString(
				HasEquivalenceString hasEquivalenceString) {
			return hasEquivalenceString == null ? "<null>"
					: hasEquivalenceString.equivalenceString();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface HasEquivalenceInfo {
		 String	value() ;
	}
	

	public static final Converter<HasEquivalenceString, String> TO_EQUIVALENCE_STRING_CONVERTER = new Converter<HasEquivalenceString, String>() {
		@Override
		public String convert(HasEquivalenceString original) {
			return original.equivalenceString();
		}
	};
}
