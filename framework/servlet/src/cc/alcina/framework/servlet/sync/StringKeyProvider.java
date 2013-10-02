package cc.alcina.framework.servlet.sync;

import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.beans.Converter;

public interface StringKeyProvider<T> {
	public String firstKey(T object);

	public List<String> allKeys(T object);

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
