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
			if (value > MAX || value < 0) {
				throw new RuntimeException("losing higher bits from long: "
						+ value);
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
			@cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash::logAndThrowTooLarge(Ljava/lang/Object;)(value);
		}
		if (value.m != 0) {
			if (value.m > @cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash::MASK_M) {
				@cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash::logAndThrowTooLarge(Ljava/lang/Object;)(value);
			}
			// << precedence < +/- !!
			return (value.m << @cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash::BITS)
					| value.l;
		}
		return value.l;
	}-*/;

	public static native void logAndThrowTooLarge(Object value)/*-{
		debugger;
		var message = "losing higher bits from long: " + value.h + "," + value.m + ","
				+ value.l;
		($wnd['console']) && console.log(message);
		throw message;
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