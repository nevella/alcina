package cc.alcina.framework.gwt.client.impl;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.service.Common;
import cc.alcina.framework.common.client.util.SimpleStringParser;
import cc.alcina.framework.gwt.client.util.AlcinaBeanSerializer;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

@SuppressWarnings("deprecation")
public class CommonExtensionsImpl implements Common._Extensions {
	// Keep synchronized with LongLib
	private static final double TWO_PWR_16_DBL = 0x10000;

	// Keep synchronized with LongLib
	private static final double TWO_PWR_32_DBL = TWO_PWR_16_DBL
			* TWO_PWR_16_DBL;

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

	@Override
	public <V> V deserializeV1(String serialized) {
		return AlcinaBeanSerializer.deserializeHolder(serialized);
	}

	@Override
	public SearchDefinition deserializeSearchDefinition(
			Map<String, Class> abbrevLookup,
			Map<Class, String> reverseAbbrevLookup, String serializedDef) {
		return Registry.impl(AlcinaBeanSerializer.class)
				.registerLookups(abbrevLookup, reverseAbbrevLookup)
				.deserialize(serializedDef);
	}

	@Override
	public String serializeSearchDefinition(Map<String, Class> abbrevLookup,
			Map<Class, String> reverseAbbrevLookup, SearchDefinition def) {
		return Registry.impl(AlcinaBeanSerializer.class)
				.registerLookups(abbrevLookup, reverseAbbrevLookup)
				.serialize(def);
	}

	public String longToGwtDoublesToString(long fieldValue) {
		double[] parts;
		if (GWT.isScript()) {
			parts = makeLongComponents0(fieldValue);
		} else {
			parts = makeLongComponents((int) (fieldValue >> 32),
					(int) fieldValue);
		}
		return parts[0] + "/" + parts[1];
	}

	protected static double[] makeLongComponents(int highBits, int lowBits) {
		double high = highBits * TWO_PWR_32_DBL;
		double low = lowBits;
		if (lowBits < 0) {
			low += TWO_PWR_32_DBL;
		}
		return new double[] { low, high };
	}

	@UnsafeNativeLong
	// Keep synchronized with LongLib
	private static native double[] makeLongComponents0(long value) /*-{
    return value;
	}-*/;

	@UnsafeNativeLong
	private static native long readLong0(double low, double high) /*-{
    return [ low, high ];
	}-*/;

	@Override
	public long gwtSerLongToString(String str) {
		int idx = str.indexOf("/");
		if (idx == -1) {
			return Long.parseLong(str);
		} else {
			double d1 = Double.parseDouble(str.substring(0, idx));
			double d2 = Double.parseDouble(str.substring(idx + 1));
			if (GWT.isScript()) {
				return readLong0(d1, d2);
			} else {
				return (long) d1 + (long) d2;
			}
		}
	}

	@Override
	public String stringify(JsonValue jsonValue, int i) {
		return JsonUtil.stringify(jsonValue, 2);
	}
}
