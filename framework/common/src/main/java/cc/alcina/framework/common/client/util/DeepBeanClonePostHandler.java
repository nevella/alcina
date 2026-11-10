package cc.alcina.framework.common.client.util;

public interface DeepBeanClonePostHandler<T> {
	public void postClone(T t);
}
