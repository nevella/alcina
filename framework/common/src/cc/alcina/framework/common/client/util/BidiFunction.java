package cc.alcina.framework.common.client.util;

import java.util.function.Function;

public interface BidiFunction<A, B> extends Function<A, B> {
	@Override
	default B apply(A a) {
		return leftToRight().apply(a);
	}

	Function<A, B> leftToRight();

	Function<B, A> rightToLeft();
}
