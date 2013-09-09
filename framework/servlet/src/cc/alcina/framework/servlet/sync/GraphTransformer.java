package cc.alcina.framework.servlet.sync;

public interface GraphTransformer<A,B> {

	B transform(A source);
}
