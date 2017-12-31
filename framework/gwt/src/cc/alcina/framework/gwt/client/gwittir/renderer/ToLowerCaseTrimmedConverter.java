package cc.alcina.framework.gwt.client.gwittir.renderer;

import com.totsp.gwittir.client.beans.Converter;

public class ToLowerCaseTrimmedConverter implements Converter<Object, String> {
	@Override
	public String convert(Object original) {
		return original == null ? null
				: original.toString().toLowerCase().trim();
	}
}
