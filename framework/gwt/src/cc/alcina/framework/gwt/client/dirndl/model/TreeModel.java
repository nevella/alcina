package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.IdentityFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.TopicBehaviourType;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.handler.EmitTopicHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.CollectionNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererArgs;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererLeaf;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent.CodeTopic;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.DomainViewNodeModel.Generator;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.LabelClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.ToggleButtonClicked;

@Directed(tag = "div", cssClass = "dl-tree", bindings = {
		@Binding(from = "hideRoot", type = Type.CSS_CLASS, literal = "hide-root") }, behaviours = {
				@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviourType.RECEIVE)),
				@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviourType.RECEIVE)),
				@Behaviour(handler = EmitTopicHandler.class, event = TopicEvent.class, topics = {
						@TopicBehaviour(topic = CodeTopic.class, type = TopicBehaviourType.RECEIVE),
						@TopicBehaviour(topic = SelectionChanged.class, type = TopicBehaviourType.EMIT), }) })
public class TreeModel<NM extends NodeModel<NM>> extends Model
		implements NodeEvent.Handler {
	private boolean hideRoot;

	private NM root;

	protected NM selectedNodeModel;

	@Directed
	public NM getRoot() {
		return this.root;
	}

	public boolean isHideRoot() {
		return this.hideRoot;
	}

	@Override
	public void onEvent(Context eventContext) {
		DirectedLayout.Node eventSource = (Node) eventContext.topicEvent.payload;
		// FIXME - dirndl1.1 - reemit at NodeModel rather than use ancestormodel
		NM model = eventSource.ancestorModel(m -> m instanceof NodeModel);
		if (eventContext.topicEvent.topic == ToggleButtonClicked.class) {
			model.setOpen(!model.isOpen());
			if (model.isOpen() && !model.populated) {
				model.populated = true;
				loadChildren(model);
			}
		}
		if (eventContext.topicEvent.topic == LabelClicked.class) {
			if (selectedNodeModel != null) {
				selectedNodeModel.setSelected(false);
			}
			selectedNodeModel = model;
			selectedNodeModel.setSelected(true);
			TopicEvent.fire(eventContext, SelectionChanged.class,
					IdentityFunction.class, model, true);
		}
	}

	public void setHideRoot(boolean hideRoot) {
		this.hideRoot = hideRoot;
	}

	public void setRoot(NM root) {
		NM old_root = this.root;
		this.root = root;
		propertyChangeSupport().firePropertyChange("root", old_root, root);
	}

	protected void loadChildren(NM model) {
	}

	public static class DomainViewNodeModel
			extends NodeModel<DomainViewNodeModel> {
		private DomainViewNode<?> node;

		private TreePath<DomainViewNodeModel> treePath;

		public DomainViewNodeModel() {
		}

		public DomainViewNodeModel(DomainViewNodeModel parent, String path) {
			setParent(parent);
			if (parent == null) {
				treePath = TreePath.root(path);
			} else {
				treePath = parent.treePath.atPath(path);
			}
			treePath.setValue(this);
		}

		public DomainViewNodeModel ensureNode(DomainViewNode valueModel,
				String path, int initialIndex,
				boolean fireCollectionModificationEvents) {
			TreePath<DomainViewNodeModel> otherTreePath = treePath.atPath(path);
			if (otherTreePath.getValue() == null) {
				DomainViewNodeModel parent = otherTreePath.getParent() == null
						? null
						: otherTreePath.getParent().getValue();
				DomainViewNodeModel model = provideContainingTree().generator
						.generate(valueModel, parent, path);
				if (parent != null) {
					parent.modifyChildren(Operation.INSERT, initialIndex, model,
							fireCollectionModificationEvents);
				}
				otherTreePath.setValue(model);
			}
			return otherTreePath.getValue();
		}

		public DomainViewNode<?> getNode() {
			return this.node;
		}

		public TreePath<DomainViewNodeModel> getTreePath() {
			return this.treePath;
		}

		public DomainViewTreeModel provideContainingTree() {
			return getTreePath().provideContainingTree();
		}

		public void putTree(DomainViewTreeModel tree) {
			getTreePath().putTree(tree);
		}

		public void removeFromParent() {
			if (getParent() != null) {
				getParent().modifyChildren(Operation.REMOVE, -1, this, true);
			}
			setParent(null);
		}

		public void setNode(DomainViewNode<?> node) {
			this.node = node;
			DomainViewTreeModel treeModel = getTreePath()
					.provideContainingTree();
			constructLabel(node);
			getLabel().setTitle(node.getTitle());
			setLeaf(node.isLeaf());
		}

		private void modifyChildren(Operation operation, int initialIndex,
				DomainViewNodeModel model,
				boolean fireCollectionModificationEvents) {
			List<NodeModel<DomainViewNodeModel>> newValue = getChildren();
			if (fireCollectionModificationEvents) {
				newValue = new ArrayList<>(newValue);
			}
			switch (operation) {
			case INSERT:
				if (initialIndex == newValue.size()) {
					newValue.add(model);
				} else {
					newValue.add(initialIndex, model);
				}
				break;
			case REMOVE:
				newValue.remove(model);
				break;
			default:
				throw new UnsupportedOperationException();
			}
			setChildren(newValue);
		}

		protected void constructLabel(DomainViewNode<?> node) {
			getLabel().setLabel(node.getName());
		}

		public static class FullLabel extends DomainViewNodeModel {
			public FullLabel() {
			}

			public FullLabel(DomainViewNodeModel parent, String path) {
				super(parent, path);
			}

			@Override
			protected void constructLabel(DomainViewNode<?> node) {
				getLabel().setLabel(node);
			}
		}

		@ClientInstantiable
		// TODO - the dirndl way would be scoped annotation resolution which
		// resolves NodeLabel.getLabel to either return the name or the object
		// itself...
		public static class Generator {
			public DomainViewNodeModel generate(DomainViewNode valueModel,
					DomainViewNodeModel parent, String path) {
				return new DomainViewNodeModel(parent, path);
			}

			public static class FullLabel extends Generator {
				@Override
				public DomainViewNodeModel generate(DomainViewNode valueModel,
						DomainViewNodeModel parent, String path) {
					return new DomainViewNodeModel.FullLabel(parent, path);
				}
			}
		}
	}

	/*
	 * Non-abstract to export reflected annotations
	 */
	public static class DomainViewTreeModel
			extends TreeModel<DomainViewNodeModel> {
		private Generator generator = new Generator();

		private TreePath<DomainViewNodeModel> openingToPath = null;

		private boolean depthFirst;

		public Generator getGenerator() {
			return this.generator;
		}

		public boolean isDepthFirst() {
			return this.depthFirst;
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

		public void setGenerator(Generator generator) {
			this.generator = generator;
		}

		protected void apply(Transform transform,
				boolean fireCollectionModificationEvents) {
			DomainViewNodeModel node = getRoot().ensureNode(transform.getNode(),
					transform.getTreePath(), transform.getIndex(),
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

		protected void handleResponse(DomainViewNode.Response response) {
			DomainViewNodeModel root = null;
			DomainViewNodeModel target = null;
			Request<?> request = response.getRequest();
			// TODO - iterate through transactions => only last one is 'replace'
			String requestPath = response.getRequest().getTreePath();
			root = (DomainViewNodeModel) getRoot();
			root.putTree(this);
			DomainViewNode rootModel = response.getTransforms().isEmpty() ? null
					: response.getTransforms().get(0).getNode();
			target = root.ensureNode(rootModel, requestPath, -1, false);
			if (response.getTransforms().isEmpty()) {
				// no children - request path has been removed in a prior tx
				return;
			}
			response.getTransforms().forEach(t -> this.apply(t, false));
			// delta children at the end to generate visual nodes after node
			// tree complete
			target.setChildren(new IdentityArrayList<>(target.getChildren()));
			if (openingToPath != null) {
				openToPath(null);
			}
		}
	}

	/**
	 * Overrides List.equals to force a property change (plus, list.equals is
	 * expensive)
	 * 
	 * @author nick@alcina.cc
	 *
	 * @param <T>
	 */
	public static class IdentityArrayList<T> extends ArrayList<T> {
		public IdentityArrayList() {
			super();
		}

		public IdentityArrayList(Collection<? extends T> c) {
			super(c);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}
	}

	@Directed(tag = "div", cssClass = "dl-tree-node", bindings = {
			@Binding(from = "open", type = Type.CSS_CLASS, literal = "open"),
			@Binding(from = "selected", type = Type.CSS_CLASS, literal = "selected"),
			@Binding(from = "leaf", type = Type.CSS_CLASS, literal = "leaf") })
	public static class NodeModel<NM extends NodeModel> extends Model {
		public boolean populated;

		private boolean open;

		private NodeLabel label = new NodeLabel();

		private List<NodeModel<NM>> children = new IdentityArrayList<>();

		private NM parent;

		private boolean leaf;

		private boolean selected;

		@Directed(renderer = MultipleNodeRenderer.class)
		@MultipleNodeRendererArgs(tags = { "div" }, cssClasses = { "" })
		@MultipleNodeRendererLeaf(@Directed(renderer = CollectionNodeRenderer.class))
		public List<NodeModel<NM>> getChildren() {
			return this.children;
		}

		@Directed
		public NodeLabel getLabel() {
			return this.label;
		}

		public NM getParent() {
			return this.parent;
		}

		public boolean isLeaf() {
			return this.leaf;
		}

		public boolean isOpen() {
			return this.open;
		}

		public boolean isSelected() {
			return this.selected;
		}

		public void setChildren(List<NodeModel<NM>> children) {
			List<NodeModel<NM>> old_children = this.children;
			this.children = children;
			propertyChangeSupport().firePropertyChange("children", old_children,
					children);
		}

		public void setLeaf(boolean leaf) {
			this.leaf = leaf;
		}

		public void setOpen(boolean open) {
			boolean old_open = this.open;
			this.open = open;
			propertyChangeSupport().firePropertyChange("open", old_open, open);
		}

		public void setParent(NM parent) {
			this.parent = parent;
		}

		public void setSelected(boolean selected) {
			boolean old_selected = this.selected;
			this.selected = selected;
			propertyChangeSupport().firePropertyChange("selected", old_selected,
					selected);
		}

		public static class LabelClicked extends NodeTopic {
		}

		@Directed(tag = "label", bindings = {
				@Binding(from = "title", to = "title", type = Binding.Type.PROPERTY) })
		public static class NodeLabel extends Model {
			private Object toggle = new Object();

			private Object label = "";

			private String title;

			@Directed(tag = "label", behaviours = {
					@Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = {
							@TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviourType.EMIT) }) })
			public Object getLabel() {
				return this.label;
			}

			public String getTitle() {
				return this.title;
			}

			@Directed(tag = "span", behaviours = {
					@Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = {
							@TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviourType.EMIT) }) })
			public Object getToggle() {
				return this.toggle;
			}

			public void setLabel(Object label) {
				Object old_label = this.label;
				this.label = label;
				propertyChangeSupport().firePropertyChange("label", old_label,
						label);
			}

			public void setTitle(String title) {
				String old_title = this.title;
				this.title = title;
				propertyChangeSupport().firePropertyChange("title", old_title,
						title);
			}
		}

		public static class SelectionChanged extends NodeTopic {
		}

		public static class ToggleButtonClicked extends NodeTopic {
		}
	}
}
