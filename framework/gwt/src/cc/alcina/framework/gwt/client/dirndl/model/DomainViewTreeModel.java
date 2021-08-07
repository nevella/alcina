package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContentModel;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContentModel.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContentModel.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContentModel.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContentModel.WaitPolicy;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTreeModel.DomainViewNodeModel;
import cc.alcina.framework.gwt.client.dirndl.model.DomainViewTreeModel.DomainViewNodeModel.Generator;

/*
 * Non-abstract to export reflected annotations
 */
public class DomainViewTreeModel extends TreeModel<DomainViewNodeModel> {
	DomainViewNodeModel.Generator generator = new Generator();

	private TreePath<DomainViewNodeModel> openingToPath = null;

	private boolean depthFirst;

	private DomainViewNodeContentModel.Response lastResponse;

	public DomainViewNodeModel.Generator getGenerator() {
		return this.generator;
	}

	public DomainViewNodeContentModel.Response getLastResponse() {
		return this.lastResponse;
	}

	public boolean isDepthFirst() {
		return this.depthFirst;
	}

	public void mergeResponse(DomainViewNodeContentModel.Response response) {
		DomainViewNodeModel root = null;
		DomainViewNodeModel target = null;
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
		Request<?> request = response.getRequest();
		// TODO - iterate through transactions => only last one is 'replace'
		// (i.e. we don't need to render nodes > once, if a node changes in
		// multiple txs in this response. But the server would be a better
		// place to implement this filtering...
		String requestPath = response.getRequest().getTreePath();
		root = (DomainViewNodeModel) getRoot();
		root.putTree(this);
		if (requestPath != null) {
			DomainViewNodeContentModel rootModel = response.getTransforms()
					.isEmpty() ? null
							: response.getTransforms().get(0).getNode();
			target = root.ensureNode(rootModel, requestPath, null, false);
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
				target.setChildren(
						new IdentityArrayList<>(target.getChildren()));
			}
			if (openingToPath != null) {
				openToPath(null);
			}
		}
		// if we're not displaying all nodes, show the paginator
		if (response.getTotalNodeCount() > root.getTreePath()
				.provideTotalNodeCount() && isDepthFirst()) {
			Paginator paginator = new Paginator();
			paginator.setText("Loading ...");
			setPaginator(paginator);
		} else {
			setPaginator(null);
		}
		setLastResponse(response);
	}

	public void openToPath(TreePath<DomainViewNodeModel> initialPath) {
		if (initialPath != null) {
			openingToPath = initialPath;
		}
		boolean initialCall = initialPath != null;
		TreePath<DomainViewNodeModel> path = openingToPath;
		DomainViewNodeModel nodeModel = path.getValue();
		if (nodeModel != null) {
			selectedNodeModel = nodeModel;
			nodeModel.setSelected(true);
			return;
		} else {
			while (nodeModel == null) {
				path = path.getParent();
				nodeModel = path.getValue();
			}
			if (nodeModel.isOpen()) {
				if (!initialCall) {
					openingToPath = null;// path not reachable
				}
				return;
			}
			{
				// FIXME - should move most event/handling down to nodemodel
				// (it fires 'requires_children')
				nodeModel.setOpen(true);
				nodeModel.populated = true;
				loadChildren(nodeModel);
			}
		}
	}

	public void sendRequest(Request<?> request) {
		throw new UnsupportedOperationException();
	}

	public void setDepthFirst(boolean depthFirst) {
		this.depthFirst = depthFirst;
	}

	public void setGenerator(DomainViewNodeModel.Generator generator) {
		this.generator = generator;
	}

	public void
			setLastResponse(DomainViewNodeContentModel.Response lastResponse) {
		DomainViewNodeContentModel.Response old_lastResponse = this.lastResponse;
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
				TreePath<DomainViewNodeModel> parent = getRoot().getTreePath()
						.ensurePath(parentPathStr);
				if (!parent.hasChildrenLoaded()) {
					return;
				}
				if (transform.getBeforePath() != null && !getRoot()
						.getTreePath().hasPath(transform.getBeforePath())) {
					return;
				}
				// TODO - (requires pagination) --
				// transform.getBeforePath()==null and not all pages
				// displayed, drop
				break;
			}
		}
		DomainViewNodeModel node = getRoot().ensureNode(transform.getNode(),
				transform.getTreePath(), transform.getBeforePath(),
				fireCollectionModificationEvents);
		switch (transform.getOperation()) {
		case INSERT:
		case CHANGE:
			node.setNode(transform.getNode());
			break;
		case REMOVE:
			node.removeFromParent();
			break;
		}
	}

	public static class DomainViewNodeModel
			extends TreeModel.NodeModel<DomainViewNodeModel> {
		private DomainViewNodeContentModel<?> node;

		private TreePath<DomainViewNodeModel> treePath;

		public DomainViewNodeModel() {
		}

		public DomainViewNodeModel(DomainViewNodeModel parent, String path) {
			setParent(parent);
			if (parent == null) {
				treePath = TreePath.absolutePath(path);
			} else {
				treePath = parent.treePath.ensurePath(path);
			}
			treePath.setValue(this);
		}

		public DomainViewNodeModel ensureNode(
				DomainViewNodeContentModel valueModel, String path,
				String beforePath, boolean fireCollectionModificationEvents) {
			TreePath<DomainViewNodeModel> otherTreePath = treePath
					.ensurePath(path);
			if (otherTreePath.getValue() == null) {
				DomainViewNodeModel parent = otherTreePath.getParent() == null
						? null
						: otherTreePath.getParent().getValue();
				Generator generator = provideContainingTree() == null
						? new Generator()
						: provideContainingTree().generator;
				DomainViewNodeModel model = generator.generate(valueModel,
						parent, path);
				if (parent != null) {
					parent.modifyChildren(Operation.INSERT, beforePath, model,
							fireCollectionModificationEvents);
				}
				otherTreePath.setValue(model);
			}
			return otherTreePath.getValue();
		}

		public DomainViewNodeContentModel<?> getNode() {
			return this.node;
		}

		public TreePath<DomainViewNodeModel> getTreePath() {
			return this.treePath;
		}

		public DomainViewTreeModel provideContainingTree() {
			return getTreePath().provideContainingTree();
		}

		public String provideLastPath() {
			DomainViewNodeModel cursor = this;
			while (cursor.getChildren().size() > 0) {
				cursor = (DomainViewNodeModel) Ax.last(cursor.getChildren());
			}
			return cursor.getTreePath().toString();
		}

		public void putTree(DomainViewTreeModel tree) {
			getTreePath().putTree(tree);
		}

		public void removeFromParent() {
			if (getParent() != null) {
				getParent().modifyChildren(Operation.REMOVE, null, this, true);
			}
			setParent(null);
		}

		public void setNode(DomainViewNodeContentModel<?> node) {
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
				DomainViewNodeModel model,
				boolean fireCollectionModificationEvents) {
			List<NodeModel<DomainViewNodeModel>> newValue = getChildren();
			if (fireCollectionModificationEvents) {
				newValue = new ArrayList<>(newValue);
			}
			switch (operation) {
			case INSERT:
				int index = newValue.size();
				if (beforePath != null) {
					if (model.getTreePath().hasPath(beforePath)) {
						DomainViewNodeModel beforePathModel = model
								.getTreePath().ensurePath(beforePath)
								.getValue();
						index = beforePathModel.getParent().getChildren()
								.indexOf(beforePathModel);
					}
				}
				if (index == newValue.size()) {
					newValue.add(model);
				} else {
					newValue.add(index, model);
				}
				break;
			case REMOVE:
				newValue.remove(model);
				model.getTreePath().removeFromParent();
				break;
			default:
				throw new UnsupportedOperationException();
			}
			setChildren(newValue);
		}

		protected void constructLabel(DomainViewNodeContentModel<?> node) {
			NodeLabelText nodeLabelText = new NodeLabelText();
			nodeLabelText.setText(node.getName());
			getLabel().setLabel(nodeLabelText);
		}

		public static class FullLabel extends DomainViewNodeModel {
			public FullLabel() {
			}

			public FullLabel(DomainViewNodeModel parent, String path) {
				super(parent, path);
			}

			@Override
			protected void constructLabel(DomainViewNodeContentModel<?> node) {
				getLabel().setLabel(node);
			}
		}

		@ClientInstantiable
		// TODO - the dirndl way would be scoped annotation resolution which
		// resolves NodeLabel.getLabel to either return the name or the object
		// itself...
		public static class Generator {
			public DomainViewNodeModel generate(
					DomainViewNodeContentModel valueModel,
					DomainViewNodeModel parent, String path) {
				return new DomainViewNodeModel(parent, path);
			}

			public static class FullLabel extends Generator {
				@Override
				public DomainViewNodeModel generate(
						DomainViewNodeContentModel valueModel,
						DomainViewNodeModel parent, String path) {
					return new DomainViewNodeModel.FullLabel(parent, path);
				}
			}
		}
	}
}