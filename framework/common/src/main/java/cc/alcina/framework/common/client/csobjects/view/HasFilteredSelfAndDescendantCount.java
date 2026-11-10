package cc.alcina.framework.common.client.csobjects.view;

public interface HasFilteredSelfAndDescendantCount<T> {
	int provideSelfAndDescendantCount(T filter);
}
