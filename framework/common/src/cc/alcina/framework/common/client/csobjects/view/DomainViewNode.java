package cc.alcina.framework.common.client.csobjects.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;

public class DomainViewNode<E extends Entity> implements HasEntity<E> {
	private E entity;

	private DomainViewNode parent;

	private TreePath treePath;

	private Set<? extends DomainViewNode<?>> children = new LightSet<>();

	public Set<? extends DomainViewNode<?>> getChildren() {
		return this.children;
	}

	public E getEntity() {
		return this.entity;
	}

	public DomainViewNode getParent() {
		return this.parent;
	}

	public TreePath getTreePath() {
		return this.treePath;
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

	public void setParent(DomainViewNode parent) {
		this.parent = parent;
	}

	public void setTreePath(TreePath treePath) {
		this.treePath = treePath;
	}

	public static class Request<D extends BindableSearchDefinition & DomainViewSearchDefinition> {
		public D searchDefinition;

		public List<TreePath> paths = new ArrayList<>();

		/*
		 * Non-null if waitPolicy == WAIT_FOR_DELTAS
		 */
		public DomainTransformCommitPosition since;

		public Type type;

		public WaitPolicy waitPolicy;
	}

	public static class Response {
		public List<Transform> transforms = new ArrayList<>();

		public boolean clearExisting;
	}

	public static class Transform {
	}

	/*
	 * For request return type specification
	 */
	public static abstract class Type {
	}

	public static enum WaitPolicy {
		RETURN_NODES, WAIT_FOR_DELTAS, CANCEL_WAITS;
	}
}
