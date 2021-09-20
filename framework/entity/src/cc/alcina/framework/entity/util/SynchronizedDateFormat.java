package cc.alcina.framework.entity.util;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class SynchronizedDateFormat extends SimpleDateFormat {
	public SynchronizedDateFormat() {
		super();
	}

	public SynchronizedDateFormat(String pattern) {
		super(pattern);
	}

	public SynchronizedDateFormat(String pattern,
			DateFormatSymbols formatSymbols) {
		super(pattern, formatSymbols);
	}

	public SynchronizedDateFormat(String pattern, Locale locale) {
		super(pattern, locale);
	}

	@Override
	public synchronized StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition pos) {
		return super.format(date, toAppendTo, pos);
	}

	@Override
	public synchronized Date parse(String source) {
		try {
			return super.parse(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public synchronized Date parse(String text, ParsePosition pos) {
		return super.parse(text, pos);
	}
}
