package cc.alcina.framework.common.client.sync;

import java.util.Collections;
import java.util.List;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface StringKeyProvider<T> {
	public String firstKey(T object);

	default List<String> allKeys(T object) {
		return Collections.singletonList(firstKey(object));
	}

	public static class StringKeyProviderAllKeysConverter<T> implements
			Converter<T, String> {
		private StringKeyProvider<T> provider;

		public StringKeyProviderAllKeysConverter(StringKeyProvider<T> provider) {
			this.provider = provider;
		}

		@Override
		public String convert(T original) {
			return CommonUtils.join(provider.allKeys(original), ", ");
		}
	}
}
