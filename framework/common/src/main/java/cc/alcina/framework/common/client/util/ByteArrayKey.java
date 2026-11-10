package cc.alcina.framework.common.client.util;

import java.util.Arrays;

public class ByteArrayKey {
	byte[] bytes;

	public ByteArrayKey(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ByteArrayKey) {
			ByteArrayKey o = (ByteArrayKey) obj;
			return Arrays.equals(bytes, o.bytes);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}
}
