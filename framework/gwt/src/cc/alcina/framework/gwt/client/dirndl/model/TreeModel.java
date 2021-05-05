package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
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
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.LabelClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.ToggleButtonClicked;

@Directed(tag = "div", cssClass = "dl-tree", bindings = {
		@Binding(from = "hideRoot", type = Type.CSS_CLASS, literal = "hide-root") }, behaviours = {
				@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviour.TopicBehaviourType.RECEIVE)),
				@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviour.TopicBehaviourType.RECEIVE)) })
public class TreeModel<NM extends NodeModel<NM>> extends Model
		implements NodeEvent.Handler {
	private boolean hideRoot;

	private NM root;

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
		NM model = eventSource.ancestorModel(m -> m instanceof NodeModel);
		if (eventContext.topicEvent.topic == ToggleButtonClicked.class) {
			model.setOpen(!model.isOpen());
			if (model.isOpen() && !model.populated) {
				model.populated = true;
				loadChildren(model);
			}
		} else {
			// TODO - fire selection event
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

		public DomainViewNodeModel ensureNode(String path, int initialIndex,
				boolean fireCollectionModificationEvents) {
			TreePath<DomainViewNodeModel> otherTreePath = treePath.atPath(path);
			if (otherTreePath.getValue() == null) {
				DomainViewNodeModel parent = otherTreePath.getParent() == null
						? null
						: otherTreePath.getParent().getValue();
				DomainViewNodeModel model = new DomainViewNodeModel(
						otherTreePath.getParent().getValue(), path);
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

		public void removeFromParent() {
			if (getParent() != null) {
				getParent().modifyChildren(Operation.REMOVE, -1, this, true);
			}
			setParent(null);
		}

		public void setNode(DomainViewNode<?> node) {
			this.node = node;
			getLabel().setLabel(node.getName());
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
	}

	public static abstract class DomainViewTreeModel
			extends TreeModel<DomainViewNodeModel> {
		public abstract void sendRequest(Request<?> request);

		protected void apply(Transform transform,
				boolean fireCollectionModificationEvents) {
			DomainViewNodeModel node = getRoot().ensureNode(
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
			target = root.ensureNode(requestPath, -1, false);
			if (response.getTransforms().isEmpty()) {
				// no children - request path has been removed in a prior tx
				return;
			}
			response.getTransforms().forEach(t -> this.apply(t, false));
			// delta children at the end to generate visual nodes after node
			// tree complete
			target.setChildren(new IdentityArrayList<>(target.getChildren()));
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
			@Binding(from = "leaf", type = Type.CSS_CLASS, literal = "leaf") })
	public static class NodeModel<NM extends NodeModel> extends Model {
		boolean populated;

		private boolean open;

		private NodeLabel label = new NodeLabel();

		private List<NodeModel<NM>> children = new IdentityArrayList<>();

		private NM parent;

		private boolean leaf;

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

		public static class LabelClicked extends NodeTopic {
		}

		@Directed(tag = "label", bindings = {
				@Binding(from = "title", to = "title", type = Binding.Type.PROPERTY) })
		public static class NodeLabel extends Model {
			private Object toggle = new Object();

			private Object label = "";

			private String title;

			@Directed(tag = "label", behaviours = @Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = @TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviour.TopicBehaviourType.EMIT)))
			public Object getLabel() {
				return this.label;
			}

			public String getTitle() {
				return this.title;
			}

			@Directed(tag = "span", behaviours = {
					@Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = {
							@TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviour.TopicBehaviourType.EMIT) }) })
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

		public static class ToggleButtonClicked extends NodeTopic {
		}
	}
}
