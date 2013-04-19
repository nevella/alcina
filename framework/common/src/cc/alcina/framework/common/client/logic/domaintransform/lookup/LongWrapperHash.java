package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;

public class LongWrapperHash {
	/* assumes >0 */
	@UnsafeNativeLong
	public static int fastIntValue(long value) {
		if (GWT.isScript()) {
			return lowBitsValue(value);
		} else {
			if (value > Integer.MAX_VALUE || value < 0) {
				throw new RuntimeException("losing higher bits from long");
			}
			return (int) value;
		}
	}

	public static final int BITS = 22;

	public static final int BITS_M = 8;

	public static final int MASK = (1 << BITS) - 1;

	public static final int MAX = (1 << (BITS + BITS_M)) - 1;

	public static final int MASK_M = (1 << BITS_M) - 1;

	@UnsafeNativeLong
	public static native int lowBitsValue(long value)/*-{
		if (value.h != 0) {
			throw new RuntimeException("losing higher bits from long");
		}
		if (value.m != 0) {
			if (value.m > MASK_M) {
				throw new RuntimeException("losing higher bits from long");
			}
			return value.m << BITS + value.l;
		}
		return value.l;
	}-*/;

	private final long value;

	private int hash;

	public LongWrapperHash(long value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LongWrapperHash) {
			return ((LongWrapperHash) obj).value == value;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = GWT.isScript() ? fastHash(value) : Long.valueOf(value)
					.hashCode();
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	@UnsafeNativeLong
	private native int fastHash(long value)/*-{
		return value.l ^ value.m ^ value.h;
	}-*/;
}