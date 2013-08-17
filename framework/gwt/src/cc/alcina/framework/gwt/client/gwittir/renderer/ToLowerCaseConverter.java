package cc.alcina.framework.gwt.client.gwittir.renderer;

import com.totsp.gwittir.client.beans.Converter;

public class ToLowerCaseConverter implements Converter<String, String> {
	@Override
	public String convert(String original) {
		return original == null ? null : original.toLowerCase();
	}
}
