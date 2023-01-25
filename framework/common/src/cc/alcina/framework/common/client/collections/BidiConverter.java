package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

public abstract interface BidiConverter<A, B> {
	public B leftToRight(A a);

	public abstract A rightToLeft(B b);

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

	default Converter<A, B> leftToRightConverter() {
		return new Converter<A, B>() {
			@Override
			public B convert(A a) {
				return leftToRight(a);
			}
		};
	}

	default Converter<B, A> rightToLeftConverter() {
		return new Converter<B, A>() {
			@Override
			public A convert(B b) {
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
