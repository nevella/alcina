package cc.alcina.framework.common.client.csobjects.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public abstract class DomainViewNodeContentModel<E extends Entity> extends Model
		implements Comparable<DomainViewNodeContentModel<E>> {
	private String name;

	private String publicationText;

	private String title;

	private transient E entity;

	private transient String __comparatorString;

	@Override
	public int compareTo(DomainViewNodeContentModel o) {
		return comparatorString().compareTo(o.comparatorString());
	}

	public abstract Class<E> entityClass();

	@AlcinaTransient
	public E getEntity() {
		return this.entity;
	}

	public String getHighestPrecedenceName() {
		return getName();
	}

	public String getName() {
		return this.name;
	}

	public String getPublicationText() {
		return this.publicationText;
	}

	public String getTitle() {
		return this.title;
	}

	public boolean isLeaf() {
		return false;
	}

	public void setEntity(E entity) {
		this.entity = entity;
	}

	public void setName(String name) {
		String old_name = this.name;
		this.name = name;
		propertyChangeSupport().firePropertyChange("name", old_name, name);
	}

	public void setPublicationText(String publicationText) {
		this.publicationText = publicationText;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", getClass().getSimpleName(),
				comparatorString());
	}

	public <DV extends DomainViewNodeContentModel<E>> DV withEntity(E entity) {
		setEntity(entity);
		return (DV) this;
	}

	private String comparatorString() {
		if (__comparatorString == null) {
			__comparatorString = comparatorString0();
			if (__comparatorString != null) {
				__comparatorString = __comparatorString.toLowerCase();
			}
		}
		return __comparatorString;
	}

	protected String comparatorString0() {
		if (title != null) {
			return title;
		}
		if (name != null) {
			return name;
		}
		if (publicationText != null) {
			return publicationText;
		}
		return null;
	}

	@ClientInstantiable
	public static enum Children {
		BREADTH_FIRST, DEPTH_FIRST, IMMEDIATE_ONLY, NONE;
	}

	public enum DefaultReturnTypes implements ReturnType {
		DEFAULT
	}

	public static abstract class EntityNode<E extends Entity>
			extends DomainViewNodeContentModel<E> implements HasEntity {
		private E entity;

		@Override
		public E getEntity() {
			return this.entity;
		}

		@Override
		public E provideEntity() {
			return getEntity();
		}

		@Override
		public void setEntity(E entity) {
			this.entity = entity;
		}
	}

	public static class Request<D extends BindableSearchDefinition & DomainViewSearchDefinition>
			extends Model {
		private D searchDefinition;

		private EntityLocator root;

		/*
		 * Non-null if waitPolicy == WAIT_FOR_DELTAS
		 */
		private DomainTransformCommitPosition since;

		private ReturnType returnType;

		private WaitPolicy waitPolicy;

		private String treePath;

		private Children children;

		private int waitId;

		// FIXME - should be a treepath ("from offest exclusive")
		private int offset;

		private int count = 100;

		public Children getChildren() {
			return this.children;
		}

		public int getCount() {
			return this.count;
		}

		public int getOffset() {
			return this.offset;
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

		public String getTreePath() {
			return this.treePath;
		}

		public int getWaitId() {
			return this.waitId;
		}

		public WaitPolicy getWaitPolicy() {
			return this.waitPolicy;
		}

		public void setChildren(Children children) {
			this.children = children;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void setOffset(int offset) {
			this.offset = offset;
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

		public void setTreePath(String treePath) {
			this.treePath = treePath;
		}

		public void setWaitId(int waitId) {
			this.waitId = waitId;
		}

		public void setWaitPolicy(WaitPolicy waitPolicy) {
			this.waitPolicy = waitPolicy;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", treePath, children);
		}
	}

	@Bean
	public static class Response implements Serializable {
		private List<Transform> transforms = new ArrayList<>();

		private DomainTransformCommitPosition position;

		private boolean clearExisting;

		private int childCount;

		private Request<?> request;

		private boolean noChangeListener;

		public Response() {
		}

		public int getChildCount() {
			return this.childCount;
		}

		public DomainTransformCommitPosition getPosition() {
			return this.position;
		}

		public Request<?> getRequest() {
			return this.request;
		}

		public List<Transform> getTransforms() {
			return this.transforms;
		}

		public boolean isClearExisting() {
			return this.clearExisting;
		}

		public boolean isNoChangeListener() {
			return this.noChangeListener;
		}

		public void setChildCount(int childCount) {
			this.childCount = childCount;
		}

		public void setClearExisting(boolean clearExisting) {
			this.clearExisting = clearExisting;
		}

		public void setNoChangeListener(boolean noChangeListener) {
			this.noChangeListener = noChangeListener;
		}

		public void setPosition(DomainTransformCommitPosition position) {
			Preconditions.checkNotNull(position);
			this.position = position;
		}

		public void setRequest(Request<?> request) {
			this.request = request;
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

	@Bean
	public static class Transform implements Serializable {
		private String treePath;

		private Operation operation;

		private DomainViewNodeContentModel node;

		private String beforePath;

		public String getBeforePath() {
			return this.beforePath;
		}

		public DomainViewNodeContentModel getNode() {
			return this.node;
		}

		public Operation getOperation() {
			return this.operation;
		}

		public String getTreePath() {
			return this.treePath;
		}

		public void setBeforePath(String beforePath) {
			this.beforePath = beforePath;
		}

		public void setNode(DomainViewNodeContentModel node) {
			this.node = node;
		}

		public void setOperation(Operation operation) {
			this.operation = operation;
		}

		public void setTreePath(String treePath) {
			this.treePath = treePath;
		}

		@Override
		public String toString() {
			String before = beforePath == null ? ""
					: Ax.format("[<<%s] ", beforePath);
			return Ax.format("%s %s%s %s", treePath, before, operation,
					node.getClass().getSimpleName());
		}
	}

	@ClientInstantiable
	public static enum WaitPolicy {
		RETURN_NODES, WAIT_FOR_DELTAS, CANCEL_WAITS;
	}
}
