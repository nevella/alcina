package cc.alcina.framework.entity.util;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SynchronizedDateFormat extends SimpleDateFormat {
	public SynchronizedDateFormat() {
		super();
	}

	public SynchronizedDateFormat(String pattern,
			DateFormatSymbols formatSymbols) {
		super(pattern, formatSymbols);
	}

	public SynchronizedDateFormat(String pattern, Locale locale) {
		super(pattern, locale);
	}

	public SynchronizedDateFormat(String pattern) {
		super(pattern);
	}

	@Override
	public synchronized Date parse(String source) throws ParseException {
		return super.parse(source);
	}

	@Override
	public synchronized Date parse(String text, ParsePosition pos) {
		return super.parse(text, pos);
	}

	@Override
	public synchronized StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition pos) {
		return super.format(date, toAppendTo, pos);
	}
}
