package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.WaitPolicy;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTree.DomainViewNode;

/*
 * Non-abstract to export reflected annotations
 */
public class DomainViewTree extends Tree<DomainViewNode> {
	DomainViewNode.LabelGenerator labelGenerator = new DomainViewNode.TextGenerator();

	private TreePath<DomainViewNode> openingToPath = null;

	private boolean depthFirst;

	private DomainViewNodeContent.Response lastResponse;

	private int selfAndDescendantCount = -1;

	public Topic<BeforeNodeRemovalEvent> beforeNodeRemoval = Topic.local();

	public DomainViewNode.LabelGenerator getLabelGenerator() {
		return this.labelGenerator;
	}

	public DomainViewNodeContent.Response getLastResponse() {
		return this.lastResponse;
	}

	public boolean isDepthFirst() {
		return this.depthFirst;
	}

	public void mergeResponse(DomainViewNodeContent.Response response) {
		DomainViewNode root = null;
		DomainViewNode target = null;
		// TODO - handle interrupt/fail
		if (response == null) {
			Response lastResponse = getLastResponse();
			setLastResponse(response);
			setLastResponse(lastResponse);
			return;
		}
		if (response.isNoChangeListener()) {
			return;
		}
		if (response.isClearExisting()) {
			reload();
			return;
		}
		Request<?> request = response.getRequest();
		// TODO - iterate through transactions => only last one is 'replace'
		// (i.e. we don't need to render nodes > once, if a node changes in
		// multiple txs in this response. But the server would be a better
		// place to implement this filtering...
		String requestPath = response.getRequest().getTreePath();
		root = (DomainViewNode) getRoot();
		root.putTree(this);
		if (requestPath != null) {
			DomainViewNodeContent rootContent = response.getTransforms()
					.isEmpty() ? null
							: response.getTransforms().get(0).getNode();
			target = root.ensureNode(rootContent, requestPath, null, false);
		}
		// TODO - requestPath ....hmmm, if switching backends, probably just
		// do a redraw/open to ...
		//
		// ...elaborating on this, still need to handle a 'refresh your
		// view' command owing to backend change
		if (response.getTransforms().isEmpty()) {
			// no children - request path has been removed in a prior tx
		} else {
			response.getTransforms()
					.forEach(t -> this.apply(t, request.getWaitPolicy()));
			// delta children at the end to generate visual nodes after node
			// tree complete
			if (requestPath != null) {
				IdentityArrayList<TreeNode<DomainViewNode>> forceEmitEvent = new IdentityArrayList<>(
						target.getChildren());
				boolean mustSetTwice = target.getChildren()
						.equals(forceEmitEvent);
				target.setChildren(forceEmitEvent);
				if (mustSetTwice) {
					target.setChildren(
							new IdentityArrayList<>(target.getChildren()));
				}
			}
			if (openingToPath != null) {
				openToPath(null);
			}
		}
		if (isDepthFirst()
				&& request.getWaitPolicy() == WaitPolicy.RETURN_NODES) {
			selfAndDescendantCount = response.getSelfAndDescendantCount();
			// FIXME - dirndl 1.3 - not sure about the logic for which
			// selfAndDescendantCount...in fact, this may all be overly complex
			// & getTransforms().size() is fine?
			if (GWT.isClient()) {
				ClientNotifications.get().log(
						"Det. paginator :: depth-first: %s - selfAndDescendantCount: %s - transforms: %s",
						isDepthFirst(), selfAndDescendantCount,
						root.getTreePath().getSelfAndDescendantCount(),
						response.getTransforms().size(), response.getRequest());
			}
			// if (selfAndDescendantCount > root.getTreePath()
			// .getSelfAndDescendantCount()) {
			if (response.getTransforms().size() > 0) {
				Paginator paginator = new Paginator();
				paginator.setText("Loading ...");
				setPaginator(paginator);
			} else {
				setPaginator(null);
			}
		}
		setLastResponse(response);
	}

	public void openToPath(TreePath<DomainViewNode> initialPath) {
		if (initialPath != null) {
			openingToPath = initialPath;
		}
		boolean initialCall = initialPath != null;
		TreePath<DomainViewNode> path = openingToPath;
		DomainViewNode node = path.getValue();
		if (node != null) {
			selectedNodeModel = node;
			node.setSelected(true);
			return;
		} else {
			while (node == null) {
				path = path.getParent();
				node = path.getValue();
			}
			if (node.isOpen()) {
				if (!initialCall) {
					openingToPath = null;// path not reachable
				}
				return;
			}
			{
				// FIXME - should move most event/handling down to node
				// (it fires 'requires_children')
				node.setOpen(true);
				node.populated = true;
				loadChildren(node);
			}
		}
	}

	public void reload() {
		getRoot().clearChildren();
		getRoot().getTreePath().clearNonRoot();
		loadChildren(getRoot());
	}

	public void sendRequest(Request<?> request) {
		throw new UnsupportedOperationException();
	}

	public void setDepthFirst(boolean depthFirst) {
		this.depthFirst = depthFirst;
	}

	public void
			setLabelGenerator(DomainViewNode.LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}

	public void setLastResponse(DomainViewNodeContent.Response lastResponse) {
		DomainViewNodeContent.Response old_lastResponse = this.lastResponse;
		this.lastResponse = lastResponse;
		propertyChangeSupport().firePropertyChange("lastResponse",
				old_lastResponse, lastResponse);
	}

	protected void apply(Transform transform, WaitPolicy waitPolicy) {
		boolean fireCollectionModificationEvents = waitPolicy == WaitPolicy.WAIT_FOR_DELTAS;
		if (waitPolicy == WaitPolicy.WAIT_FOR_DELTAS) {
			// don't apply delta transforms if outside the visible tree
			switch (transform.getOperation()) {
			case REMOVE:
			case CHANGE:
				if (!getRoot().getTreePath().hasPath(transform.getTreePath())) {
					return;
				}
				break;
			case INSERT:
				String parentPathStr = TreePath
						.parentPath(transform.getTreePath());
				if (!getRoot().getTreePath().hasPath(parentPathStr)) {
					return;
				}
				TreePath<DomainViewNode> parent = getRoot().getTreePath()
						.ensurePath(parentPathStr);
				if (!parent.hasChildrenLoaded() && !isDepthFirst()) {
					return;
				}
				if (transform.getBeforePath() != null && !getRoot()
						.getTreePath().hasPath(transform.getBeforePath())) {
					return;
				}
				break;
			}
		}
		DomainViewNode node = getRoot().ensureNode(transform.getNode(),
				transform.getTreePath(), transform.getBeforePath(),
				fireCollectionModificationEvents);
		switch (transform.getOperation()) {
		case INSERT:
		case CHANGE:
			node.setNode(transform.getNode());
			break;
		case REMOVE:
			TreePath next = node.getTreePath().walker().next();
			if (next == null) {
				next = node.getTreePath().walker().previous();
			}
			beforeNodeRemoval.publish(new BeforeNodeRemovalEvent(
					node.getTreePath(), next == null ? null : next));
			node.removeFromParent();
			break;
		}
	}

	public class BeforeNodeRemovalEvent {
		public TreePath removed;

		public TreePath next;

		public BeforeNodeRemovalEvent(TreePath removed, TreePath next) {
			super();
			this.removed = removed;
			this.next = next;
		}
	}

	public static class DomainViewNode extends Tree.TreeNode<DomainViewNode> {
		private DomainViewNodeContent<?> node;

		private TreePath<DomainViewNode> treePath;

		private LabelGenerator labelGenerator;

		public DomainViewNode() {
		}

		public DomainViewNode(LabelGenerator labelGenerator,
				DomainViewNode parent, String path) {
			this.labelGenerator = labelGenerator == null ? new TextGenerator()
					: labelGenerator;
			setParent(parent);
			if (parent == null) {
				treePath = TreePath.absolutePath(path);
			} else {
				treePath = parent.treePath.ensurePath(path);
			}
			treePath.setValue(this);
		}

		public void clearChildren() {
			List<DomainViewNode> list = (List) getChildren().stream()
					.collect(Collectors.toList());
			list.forEach(DomainViewNode::removeFromParent);
			populated = false;
		}

		public DomainViewNode ensureNode(DomainViewNodeContent nodeContent,
				String path, String beforePath,
				boolean fireCollectionModificationEvents) {
			TreePath<DomainViewNode> otherTreePath = treePath.ensurePath(path);
			if (otherTreePath.getValue() == null) {
				DomainViewNode parent = otherTreePath.getParent() == null ? null
						: otherTreePath.getParent().getValue();
				LabelGenerator labelGenerator = provideContainingTree() == null
						? null
						: provideContainingTree().labelGenerator;
				DomainViewNode node = new DomainViewNode(labelGenerator, parent,
						path);
				if (parent != null) {
					parent.modifyChildren(Operation.INSERT, beforePath, node,
							fireCollectionModificationEvents);
				}
				otherTreePath.setValue(node);
			}
			return otherTreePath.getValue();
		}

		public DomainViewNodeContent<?> getNode() {
			return this.node;
		}

		public TreePath<DomainViewNode> getTreePath() {
			return this.treePath;
		}

		public DomainViewTree provideContainingTree() {
			return getTreePath().provideContainingTree();
		}

		public String provideLastPath() {
			DomainViewNode cursor = this;
			while (cursor.getChildren().size() > 0) {
				cursor = (DomainViewNode) Ax.last(cursor.getChildren());
			}
			return cursor.getTreePath().toString();
		}

		public void putTree(DomainViewTree tree) {
			getTreePath().putTree(tree);
		}

		public void removeFromParent() {
			if (getParent() != null) {
				getParent().modifyChildren(Operation.REMOVE, null, this, true);
			}
			setParent(null);
		}

		public void setNode(DomainViewNodeContent<?> node) {
			this.node = node;
			constructLabel(node);
			getLabel().setTitle(node.getTitle());
			setLeaf(node.isLeaf());
		}

		@Override
		public String toString() {
			return Ax.format("%s [%s children]", getTreePath(),
					getChildren().size());
		}

		private void modifyChildren(Operation operation, String beforePath,
				DomainViewNode node, boolean fireCollectionModificationEvents) {
			List<TreeNode<DomainViewNode>> newValue = getChildren();
			if (fireCollectionModificationEvents) {
				newValue = new ArrayList<>(newValue);
			}
			switch (operation) {
			case INSERT:
				int index = newValue.size();
				if (beforePath != null) {
					if (node.getTreePath().hasPath(beforePath)) {
						DomainViewNode beforePathNode = node.getTreePath()
								.ensurePath(beforePath).getValue();
						index = beforePathNode.getParent().getChildren()
								.indexOf(beforePathNode);
					}
				}
				if (index == newValue.size()) {
					newValue.add(node);
				} else {
					newValue.add(index, node);
				}
				break;
			case REMOVE:
				newValue.remove(node);
				node.getTreePath().removeFromParent();
				break;
			default:
				throw new UnsupportedOperationException();
			}
			setChildren(newValue);
		}

		protected void constructLabel(DomainViewNodeContent<?> node) {
			getLabel().setLabel(labelGenerator.apply(node));
		}

		public static class ContentGenerator implements LabelGenerator {
			@Override
			public Object apply(DomainViewNodeContent<?> t) {
				return t;
			}
		}

		public static interface LabelGenerator
				extends Function<DomainViewNodeContent<?>, Object> {
		}

		public static class TextGenerator implements LabelGenerator {
			@Override
			public Object apply(DomainViewNodeContent<?> t) {
				NodeLabelText nodeLabelText = new NodeLabelText();
				nodeLabelText.setText(t.getName());
				return nodeLabelText;
			}
		}
	}
}