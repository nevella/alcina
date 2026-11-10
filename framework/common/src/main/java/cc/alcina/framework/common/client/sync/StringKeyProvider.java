package cc.alcina.framework.common.client.sync;

import java.util.Collections;
import java.util.List;

import java.util.function.Function;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface StringKeyProvider<T> {
	default List<String> allKeys(T object) {
		return Collections.singletonList(firstKey(object));
	}

	public String firstKey(T object);

	public static class StringKeyProviderAllKeysConverter<T>
			implements Function<T, String> {
		private StringKeyProvider<T> provider;

		public StringKeyProviderAllKeysConverter(
				StringKeyProvider<T> provider) {
			this.provider = provider;
		}

		@Override
		public String apply(T original) {
			return CommonUtils.join(provider.allKeys(original), ", ");
		}
	}
}
