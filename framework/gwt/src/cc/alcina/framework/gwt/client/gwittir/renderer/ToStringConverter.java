package cc.alcina.framework.gwt.client.gwittir.renderer;

import com.totsp.gwittir.client.beans.Converter;

public class ToStringConverter implements Converter<Object, String> {
	@Override
	public String convert(Object o) {
		return o == null ? "null" : o.toString();
	}
}
