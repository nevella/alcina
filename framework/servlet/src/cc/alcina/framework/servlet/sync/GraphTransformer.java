package cc.alcina.framework.servlet.sync;

public interface GraphTransformer<A, B> {
	B transform(A source);

	public static class PassthroughTransformer<A>
			implements GraphTransformer<A, A> {
		@Override
		public A transform(A source) {
			return source;
		}
	}
}
