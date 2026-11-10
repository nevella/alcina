package cc.alcina.framework.gwt.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;

import cc.alcina.framework.common.client.service.CommonExtensions;
import cc.alcina.framework.common.client.util.SimpleStringParser;

public class CommonExtensionsImpl extends CommonExtensions {
	@Override
	public String serializeLongToJs(long value) {
		String serLong = GWT.isScript() ? fastMinSerialisedLong(value)
				: String.valueOf(value);
		return serLong + "/" + SimpleStringParser.toBase64(value);
	}

	@UnsafeNativeLong
	private static native String fastMinSerialisedLong(long value)/*-{
																	if (value.l === undefined) {
																	return value + "";
																	}
																	if (!value.m && !value.h) {
																	return value.l.toString();
																	} else {
																	return value.l.toString() + "(lsbits)";
																	}
																	}-*/;
}
