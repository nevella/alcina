package cc.alcina.framework.common.client.util;

import java.util.function.Function;

public class Functions {
	public static final Function<Object, String> TO_STRING_CONVERTER = new Function<Object, String>() {
		@Override
		public String apply(Object original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Function<String, Double> STRING_TO_DOUBLE_CONVERTER = new Function<String, Double>() {
		@Override
		public Double apply(String original) {
			return (original == null) ? null : Double.valueOf(original);
		}
	};

	public static final Function<Double, String> DOUBLE_TO_STRING_CONVERTER = new Function<Double, String>() {
		@Override
		public String apply(Double original) {
			return (original == null) ? null : original.toString();
		}
	};
}
