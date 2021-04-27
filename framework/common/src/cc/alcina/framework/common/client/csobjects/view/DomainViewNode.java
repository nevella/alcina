package cc.alcina.framework.common.client.csobjects.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public abstract class DomainViewNode<E extends Entity> extends Model {
	private DomainViewNode parent;

	private String name;

	private TreePath treePath;

	private Set<DomainViewNode> children = new LightSet<>();

	public abstract Class<E> entityClass();

	public Set<DomainViewNode> getChildren() {
		return this.children;
	}

	public String getName() {
		return this.name;
	}

	public DomainViewNode getParent() {
		return this.parent;
	}

	public TreePath getTreePath() {
		return this.treePath;
	}

	public void setChildren(Set<DomainViewNode> children) {
		this.children = children;
	}

	public void setName(String name) {
		String old_name = this.name;
		this.name = name;
		propertyChangeSupport().firePropertyChange("name", old_name, name);
	}

	public void setParent(DomainViewNode parent) {
		this.parent = parent;
	}

	public void setTreePath(TreePath treePath) {
		this.treePath = treePath;
	}

	@ClientInstantiable
	public static enum Children {
		BREADTH_FIRST, DEPTH_FIRST, IMMEDIATE_ONLY, NONE;
	}

	public enum DefaultReturnTypes implements ReturnType {
		DEFAULT
	}

	public static abstract class EntityNode<E extends Entity>
			extends DomainViewNode<E> implements HasEntity {
		private E entity;

		public E getEntity() {
			return this.entity;
		}

		@Override
		public E provideEntity() {
			return getEntity();
		}

		public void setEntity(E entity) {
			this.entity = entity;
		}
	}

	public static class Request<D extends BindableSearchDefinition & DomainViewSearchDefinition>
			implements Serializable {
		private D searchDefinition;

		private EntityLocator root;

		private List<Element> elements = new ArrayList<>();

		/*
		 * Non-null if waitPolicy == WAIT_FOR_DELTAS
		 */
		private DomainTransformCommitPosition since;

		private ReturnType returnType;

		private WaitPolicy waitPolicy;

		public void addElement(TreePath path) {
			Element elem = new Element();
			elem.setPath(path);
			getElements().add(elem);
		}

		public List<Element> getElements() {
			return this.elements;
		}

		public ReturnType getReturnType() {
			return this.returnType;
		}

		public EntityLocator getRoot() {
			return this.root;
		}

		public D getSearchDefinition() {
			return this.searchDefinition;
		}

		public DomainTransformCommitPosition getSince() {
			return this.since;
		}

		public WaitPolicy getWaitPolicy() {
			return this.waitPolicy;
		}

		public void setElements(List<Element> elements) {
			this.elements = elements;
		}

		public void setReturnType(ReturnType returnType) {
			this.returnType = returnType;
		}

		public void setRoot(EntityLocator root) {
			this.root = root;
		}

		public void setSearchDefinition(D searchDefinition) {
			this.searchDefinition = searchDefinition;
		}

		public void setSince(DomainTransformCommitPosition since) {
			this.since = since;
		}

		public void setWaitPolicy(WaitPolicy waitPolicy) {
			this.waitPolicy = waitPolicy;
		}

		@Bean
		public static class Element implements Serializable {
			private TreePath path;

			private Children children = Children.IMMEDIATE_ONLY;

			public Children getChildren() {
				return this.children;
			}

			public TreePath getPath() {
				return this.path;
			}

			public void setChildren(Children children) {
				this.children = children;
			}

			public void setPath(TreePath path) {
				this.path = path;
			}

			@Override
			public String toString() {
				return Ax.format("%s :: %s", path, children);
			}
		}
	}

	@Bean
	public static class Response implements Serializable {
		private List<Transform> transforms = new ArrayList<>();

		private boolean clearExisting;

		public List<Transform> getTransforms() {
			return this.transforms;
		}

		public boolean isClearExisting() {
			return this.clearExisting;
		}

		public void setClearExisting(boolean clearExisting) {
			this.clearExisting = clearExisting;
		}

		public void setTransforms(List<Transform> transforms) {
			this.transforms = transforms;
		}
	}

	/*
	 * For request return type specification
	 */
	public interface ReturnType {
	}

	public static class Top extends DomainViewNode {
		public Top() {
			setTreePath(new TreePath());
		}

		@Override
		public Class entityClass() {
			return null;
		}
	}

	@Bean
	public static class Transform implements Serializable {
		private TreePath path;

		private Type type;

		private DomainViewNode node;

		private String newPropertyStringValue;

		public String getNewPropertyStringValue() {
			return this.newPropertyStringValue;
		}

		public DomainViewNode getNode() {
			return this.node;
		}

		public TreePath getPath() {
			return this.path;
		}

		public Type getType() {
			return this.type;
		}

		public void setNewPropertyStringValue(String newPropertyStringValue) {
			this.newPropertyStringValue = newPropertyStringValue;
		}

		public void setNode(DomainViewNode node) {
			this.node = node;
		}

		public void setPath(TreePath path) {
			this.path = path;
		}

		public void setType(Type type) {
			this.type = type;
		}

		@ClientInstantiable
		public static enum Type {
			APPEND, INSERT_AFTER, REMOVE, SET_PROPERTY
		}
	}

	@ClientInstantiable
	public static enum WaitPolicy {
		RETURN_NODES, WAIT_FOR_DELTAS, CANCEL_WAITS;
	}
}
