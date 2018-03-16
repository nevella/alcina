package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.collections.BidiConverter;

public class BidiLongConverter extends BidiConverter<String, Long> {
	private boolean preserveNulls;

	boolean ignoreZeroes = false;

	public BidiLongConverter(boolean preserveNulls) {
		this.preserveNulls = preserveNulls;
	}

	public BidiLongConverter ignoreZeroes() {
		ignoreZeroes = true;
		return this;
	}

	@Override
	public Long leftToRight(String value) {
		if (value == null) {
			return preserveNulls ? null : 0L;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0L;
		}
	}

	@Override
	public String rightToLeft(Long l) {
		return l == null ? null
				: ignoreZeroes && l == 0 ? null : String.valueOf(l);
	}
}