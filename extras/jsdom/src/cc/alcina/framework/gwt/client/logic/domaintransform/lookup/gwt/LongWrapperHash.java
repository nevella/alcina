package cc.alcina.framework.gwt.client.logic.domaintransform.lookup.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHashConstants;

public class LongWrapperHash implements LongWrapperHashConstants {
	@UnsafeNativeLong
	public static native int fastHash(long value)/*-{
    if (value.l === undefined) {
      return value;
    }
    return value.l ^ value.m ^ value.h;
	}-*/;

	/* assumes >0 */
	@UnsafeNativeLong
	public static int fastIntValue(long value) {
		if (GWT.isScript()) {
			return lowBitsValue(value);
		} else {
			if (value > MAX || value < 0) {
				throw new RuntimeException(
						"losing higher bits from long: " + value);
			}
			return (int) value;
		}
	}

	public static long fastLongValue(int value) {
		if (GWT.isScript()) {
			return longFromInt(value);
		} else {
			if (value < 0) {
				throw new RuntimeException("no negative values: " + value);
			}
			return (long) value;
		}
	}

	public static native void logAndThrowTooLarge(Object value)/*-{
    debugger;
    var message = "losing higher bits from long: " + value.h + "," + value.m
        + "," + value.l;
    ($wnd['console']) && console.log(message);
    throw message;
	}-*/;

	@UnsafeNativeLong
	public static native long longFromInt(int value)/*-{
    return @com.google.gwt.lang.LongLib::fromInt(I)(value);
	}-*/;

	@UnsafeNativeLong
	public static native int lowBitsValue(long value)/*-{
    if (value.h === undefined) {
      return value;
    }
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
			hash = GWT.isScript() ? fastHash(value)
					: Long.valueOf(value).hashCode();
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}
}