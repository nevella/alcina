package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

public abstract class BidiConverter<A, B> {
	public abstract B leftToRight(A a);

	public abstract A rightToLeft(B b);

	public Converter<A, B> leftToRightConverter() {
		return new Converter<A, B>() {
			@Override
			public B convert(A a) {
				return leftToRight(a);
			}
		};
	}

	public Converter<B, A> rightToLeftConverter() {
		return new Converter<B, A>() {
			@Override
			public A convert(B b) {
				return rightToLeft(b);
			}
		};
	}
	public static class BidiIdentityConverter<A> extends BidiConverter<A, A>{

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
