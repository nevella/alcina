package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.util.Ax;

public class BidiPrefixedLongConverter extends BidiConverter<String, Long> {
	private boolean preserveNulls;

	boolean ignoreZeroes = false;

	private String prefix;

	public BidiPrefixedLongConverter(String prefix, boolean preserveNulls) {
		this.prefix = prefix;
		this.preserveNulls = preserveNulls;
	}

	public BidiPrefixedLongConverter ignoreZeroes() {
		ignoreZeroes = true;
		return this;
	}

	@Override
	public Long leftToRight(String value) {
		if (value == null) {
			return preserveNulls ? null : 0L;
		}
		try {
			if (value.matches("\\d+")) {
				return Long.valueOf(value);
			} else {
				if (value.matches(Ax.format("%s\\d+", prefix))) {
					return Long.valueOf(value.substring(prefix.length()));
				} else {
					return preserveNulls ? null : 0L;
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0L;
		}
	}

	@Override
	public String rightToLeft(Long l) {
		return l == null ? null
				: ignoreZeroes && l == 0 ? null : Ax.format("%s%s", prefix, l);
	}
}