package cc.alcina.framework.common.client.gwittir.validator;

import java.util.Date;

import com.totsp.gwittir.client.beans.Converter;

public class DateToLongStringConverter implements Converter<Date, String> {
	public static final DateToLongStringConverter INSTANCE = new DateToLongStringConverter();

	public String convert(Date original) {
		return original == null ? null : String.valueOf(original.getTime());
	}
}
