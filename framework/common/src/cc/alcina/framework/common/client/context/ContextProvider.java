package cc.alcina.framework.common.client.context;

public interface ContextProvider<F extends ContextFrame> {
	F contextInstance();
}
