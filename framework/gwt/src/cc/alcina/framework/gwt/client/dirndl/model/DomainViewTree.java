package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.WaitPolicy;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTree.DomainViewNode;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath.Operation;

public abstract class DomainViewTree extends Tree<DomainViewNode> {
	DomainViewNode.LabelGenerator labelGenerator = new DomainViewNode.TextGenerator();

	private TreePath<DomainViewNode> openingToPath = null;

	private boolean depthFirst;

	private DomainViewNodeContent.Response lastResponse;

	public Topic<BeforeNodeRemovalEvent> beforeNodeRemoval = Topic.create();

	public Topic<NodeChangeEvent> afterNodeChange = Topic.create();

	private boolean saveResponses;

	private List<DomainViewNodeContent.Response> savedResponses = new ArrayList<>();

	@AlcinaTransient
	public DomainViewNode.LabelGenerator getLabelGenerator() {
		return this.labelGenerator;
	}

	public DomainViewNodeContent.Response getLastResponse() {
		return this.lastResponse;
	}

	public List<DomainViewNodeContent.Response> getSavedResponses() {
		return this.savedResponses;
	}

	public boolean isDepthFirst() {
		return this.depthFirst;
	}

	/**
	 * Save responses for tree state debugging
	 */
	public boolean isSaveResponses() {
		return this.saveResponses;
	}

	public void mergeResponse(DomainViewNodeContent.Response response) {
		DomainViewNode root = null;
		DomainViewNode target = null;
		if (isSaveResponses()) {
			savedResponses.add(response);
		}
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
			/*
			 * FIXME - dirndl 1x1g - what happens if changes occur at multiple
			 * nodes?
			 *
			 * actually
			 *
			 * FIXME - dirndl 1x1g - framework this - see TreeNode.setChildren -
			 * buffer changes
			 */
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
		if (GWT.isClient() && isDepthFirst()
				&& request.getWaitPolicy() == WaitPolicy.RETURN_NODES) {
			// if there are transforms in this response, add a paginator at the
			// end (since there may be more)
			//
			// once a request (possibly triggered by
			// paginator scrolling into view) returns a zero-transform response,
			// there's no need to display the paginator
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

	public abstract void sendRequest(Request<?> request);

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

	public void setSaveResponses(boolean saveResponses) {
		this.saveResponses = saveResponses;
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
				Optional<TreePath<DomainViewNode>> parentPathOptional = getRoot()
						.getTreePath().getPath(parentPathStr);
				if (parentPathOptional.isEmpty()) {
					return;
				}
				TreePath<DomainViewNode> parentPath = parentPathOptional.get();
				if (!parentPath.hasChildrenLoaded() && !isDepthFirst()) {
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
			afterNodeChange.publish(new NodeChangeEvent(node.getTreePath()));
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

	/*
	 *
	 * Implements hasTag to allow a self tag of 'node' rather than
	 * 'domain-view-node' - but otherwise respecting subclass name
	 *
	 */
	@Directed(bindings = @Binding(from = "pathSegment", type = Type.PROPERTY))
	public static class DomainViewNode
			extends Tree.AbstractPathNode<DomainViewNode> implements HasTag {
		private DomainViewNodeContent<?> node;

		private LabelGenerator labelGenerator;

		public DomainViewNode() {
		}

		public DomainViewNode(LabelGenerator labelGenerator,
				DomainViewNode parent, String path) {
			super(parent, path, false);
			this.labelGenerator = labelGenerator == null ? new TextGenerator()
					: labelGenerator;
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

		/*
		 * Expose path segment for possible subtree css rules
		 */
		public String getPathSegment() {
			return this.node instanceof ExposePathSegment
					? getTreePath().getSegment()
					: null;
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

		@Override
		public String provideTag() {
			Class<? extends DomainViewNode> clazz = getClass();
			if (clazz == DomainViewNode.class) {
				return "node";
			} else {
				return DirectedRenderer.tagName(clazz);
			}
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
					Optional<TreePath<DomainViewNode>> path = node.getTreePath()
							.getPath(beforePath);
					if (path.isPresent()) {
						DomainViewNode beforePathNode = path.get().getValue();
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

	/*
	 * Instructs the tree renderer to expose the path segment, if the node
	 * content is of this type
	 *
	 * Note that this would normally be used for css styling of subtrees - and
	 * there are possibly better ways (i.e. use a ContextResolver to change the
	 * model, at least if deleting content)
	 */
	public interface ExposePathSegment {
	}

	/*
	 * Populated by application code, not rpc calls
	 */
	public static class Local extends DomainViewTree {
		@Override
		public void sendRequest(Request<?> request) {
			throw new UnsupportedOperationException();
		}
	}

	public class NodeChangeEvent {
		public TreePath changed;

		public NodeChangeEvent(TreePath changed) {
			super();
			this.changed = changed;
		}
	}
}