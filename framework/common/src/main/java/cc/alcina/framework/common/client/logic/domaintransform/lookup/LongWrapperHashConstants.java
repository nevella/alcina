package cc.alcina.framework.common.client.logic.domaintransform.lookup;

public interface LongWrapperHashConstants {
	public static final int BITS = 22;

	public static final int BITS_M = 8;

	public static final int MASK = (1 << BITS) - 1;

	public static final int MAX = (1 << (BITS + BITS_M)) - 1;

	public static final int MASK_M = (1 << BITS_M) - 1;
}