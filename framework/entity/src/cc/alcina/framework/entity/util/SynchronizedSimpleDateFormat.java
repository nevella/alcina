package cc.alcina.framework.entity.util;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SynchronizedSimpleDateFormat extends SimpleDateFormat {
	public SynchronizedSimpleDateFormat(String pattern) {
		super(pattern);
	}

	@Override
	public synchronized StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition pos) {
		return super.format(date, toAppendTo, pos);
	}

	@Override
	public synchronized Date parse(String source) throws ParseException {
		return super.parse(source);
	}
}