package cc.alcina.framework.common.client.csobjects.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domain.HasEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public abstract class DomainViewNodeContent<E extends Entity> extends Model
		implements Comparable<DomainViewNodeContent<E>> {
	private String name;

	private String publicationText;

	private String title;

	private transient E entity;

	private transient String __comparatorString;

	private transient Exception exception;

	@AlcinaTransient
	public Exception getException() {
		return this.exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	@Override
	public int compareTo(DomainViewNodeContent o) {
		int stringCmp = comparatorString().compareTo(o.comparatorString());
		if (stringCmp != 0) {
			return stringCmp;
		}
		if (entity != null && o.entity != null) {
			return EntityComparator.INSTANCE.compare(entity, o.getEntity());
		}
		return 0;
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

	public <DV extends DomainViewNodeContent<E>> DV withEntity(E entity) {
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

	@Reflected
	public static enum Children {
		BREADTH_FIRST, DEPTH_FIRST, IMMEDIATE_ONLY, NONE;
	}

	public enum DefaultReturnTypes implements ReturnType {
		DEFAULT
	}

	public static abstract class EntityNode<E extends Entity>
			extends DomainViewNodeContent<E> implements HasEntity {
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

		private String fromOffsetExclusivePath;

		private int count = 100;

		public Children getChildren() {
			return this.children;
		}

		public int getCount() {
			return this.count;
		}

		public String getFromOffsetExclusivePath() {
			return this.fromOffsetExclusivePath;
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

		public void setFromOffsetExclusivePath(String fromOffsetExclusivePath) {
			this.fromOffsetExclusivePath = fromOffsetExclusivePath;
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
			return Ax.format("%s :: %s :: %s", treePath, children, waitPolicy);
		}
	}

	@Bean
	public static class Response implements Serializable {
		private List<Transform> transforms = new ArrayList<>();

		private DomainTransformCommitPosition position;

		private boolean clearExisting;

		private Request<?> request;

		private boolean noChangeListener;

		private boolean delayBeforeReturn;

		private int selfAndDescendantCount;

		public Response() {
		}

		public DomainTransformCommitPosition getPosition() {
			return this.position;
		}

		public Request<?> getRequest() {
			return this.request;
		}

		public int getSelfAndDescendantCount() {
			return this.selfAndDescendantCount;
		}

		public List<Transform> getTransforms() {
			return this.transforms;
		}

		public boolean isClearExisting() {
			return this.clearExisting;
		}

		/*
		 * Server delays before returning
		 */
		public boolean isDelayBeforeReturn() {
			return this.delayBeforeReturn;
		}

		public boolean isNoChangeListener() {
			return this.noChangeListener;
		}

		public void setClearExisting(boolean clearExisting) {
			this.clearExisting = clearExisting;
		}

		public void setDelayBeforeReturn(boolean delayBeforeReturn) {
			this.delayBeforeReturn = delayBeforeReturn;
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

		public void setSelfAndDescendantCount(int selfAndDescendantCount) {
			this.selfAndDescendantCount = selfAndDescendantCount;
		}

		public void setTransforms(List<Transform> transforms) {
			this.transforms = transforms;
		}
	}

	/*
	 * For request return type specification (currently unused)
	 */
	public interface ReturnType {
	}

	@Bean
	public static class Transform implements Serializable {
		private String treePath;

		private Operation operation;

		private DomainViewNodeContent node;

		private String beforePath;

		public transient TreePath localPath;

		public Transform copy() {
			Transform transform = new Transform();
			transform.beforePath = beforePath;
			transform.localPath = localPath;
			transform.node = node;
			transform.operation = operation;
			transform.treePath = treePath;
			return transform;
		}

		public String getBeforePath() {
			return this.beforePath;
		}

		public DomainViewNodeContent getNode() {
			return this.node;
		}

		public Operation getOperation() {
			return this.operation;
		}

		public String getTreePath() {
			return this.treePath;
		}

		public void putPath(TreePath path) {
			setTreePath(path.toString());
			localPath = path;
		}

		public void setBeforePath(String beforePath) {
			this.beforePath = beforePath;
		}

		public void setNode(DomainViewNodeContent node) {
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
			return Ax.format("%s %s%s %s %s", treePath, before, operation,
					node.getClass().getSimpleName(), node.getName());
		}
	}

	@Reflected
	public static enum WaitPolicy {
		RETURN_NODES, WAIT_FOR_DELTAS, CANCEL_WAITS;
	}
}
