package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

public abstract interface BidiConverter<A, B> {
	default BidiConverter<B, A> invertBidi() {
		BidiConverter<A, B> from = this;
		return new BidiConverter<B, A>() {
			@Override
			public A leftToRight(B b) {
				return from.rightToLeft(b);
			}

			@Override
			public B rightToLeft(A a) {
				return from.leftToRight(a);
			}
		};
	}

	public B leftToRight(A a);

	default Function<A, B> leftToRightConverter() {
		return new Function<A, B>() {
			@Override
			public B apply(A a) {
				return leftToRight(a);
			}
		};
	}

	public abstract A rightToLeft(B b);

	default Function<B, A> rightToLeftConverter() {
		return new Function<B, A>() {
			@Override
			public A apply(B b) {
				return rightToLeft(b);
			}
		};
	}

	public static class BidiIdentityConverter<A>
			implements BidiConverter<A, A> {
		@Override
		public A leftToRight(A a) {
			return a;
		}

		@Override
		public A rightToLeft(A b) {
			return b;
		}
	}
}
