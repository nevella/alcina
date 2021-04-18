package cc.alcina.framework.common.client.csobjects.view;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasEntity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;

public class DomainViewNode<E extends Entity> implements HasEntity<E> {
	private E entity;

	private DomainViewNode parent;

	private TreePath treePath;

	public TreePath getTreePath() {
		return this.treePath;
	}

	public void setTreePath(TreePath treePath) {
		this.treePath = treePath;
	}

	public DomainViewNode getParent() {
		return this.parent;
	}

	public void setParent(DomainViewNode parent) {
		this.parent = parent;
	}

	private Set<? extends DomainViewNode<?>> children = new LightSet<>();

	public Set<? extends DomainViewNode<?>> getChildren() {
		return this.children;
	}

	public E getEntity() {
		return this.entity;
	}

	@Override
	public E provideEntity() {
		return getEntity();
	}

	public void setChildren(Set<? extends DomainViewNode<?>> children) {
		this.children = children;
	}

	public void setEntity(E entity) {
		this.entity = entity;
	}

	/*
	 * For request typing
	 */
	public static abstract class Type {
	}

	public static class Request{
		domainvi
	}
}
