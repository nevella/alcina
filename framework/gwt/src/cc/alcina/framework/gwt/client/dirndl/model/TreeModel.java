package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
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
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererArgs;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererLeaf;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.LabelClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel.ToggleButtonClicked;

@Directed(tag = "div", cssClass = "dl-tree")
public class TreeModel extends Model {
	// does this need to be non-null?Placeholder for Dirndl/listener?
	private NodeModel root = new NodeModel();

	@Directed
	public NodeModel getRoot() {
		return this.root;
	}

	public void setRoot(NodeModel root) {
		NodeModel old_root = this.root;
		this.root = root;
		propertyChangeSupport().firePropertyChange("root", old_root, root);
	}

	public static class DomainViewNodeModel extends NodeModel {
		private DomainViewNode<?> node;

		public DomainViewNodeModel() {
		}

		public DomainViewNodeModel(DomainViewNode node) {
			this.node = node;
			getLabel().setLabel(node.getName());
		}

		@Override
		protected void ensureChildren() {
			if (this.children == null) {
				setChildren(node.getChildren().stream()
						.map(DomainViewNodeModel::new)
						.collect(Collectors.toList()));
			}
		}
	}

	@Directed(tag = "div", cssClass = "dl-tree-node", bindings = {
			@Binding(from = "open", type = Type.CSS_CLASS, literal = "open") }, behaviours = {
					@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviour.TopicBehaviourType.RECEIVE)),
					@Behaviour(event = TopicEvent.class, topics = @TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviour.TopicBehaviourType.RECEIVE)) })
	public static class NodeModel extends Model implements NodeEvent.Handler {
		private boolean open;

		private NodeLabel label = new NodeLabel();

		protected List<NodeModel> children;

		@Directed(renderer = MultipleNodeRenderer.class)
		@MultipleNodeRendererArgs(tags = { "div" }, cssClasses = { "" })
		@MultipleNodeRendererLeaf(@Directed(renderer = CollectionNodeRenderer.class))
		public List<NodeModel> getChildren() {
			if (!isOpen()) {
				return new ArrayList<>();
			}
			ensureChildren();
			return this.children;
		}

		@Directed
		public NodeLabel getLabel() {
			return this.label;
		}

		public boolean isOpen() {
			return this.open;
		}

		@Override
		public void onEvent(Context eventContext) {
			if (eventContext.topicEvent.topic == ToggleButtonClicked.class) {
				setOpen(!isOpen());
				eventContext.cancelBubble = true;
			} else {
				// TODO - fire selection event
			}
		}

		public void setChildren(List<NodeModel> children) {
			List<NodeModel> old_children = this.children;
			this.children = children;
			propertyChangeSupport().firePropertyChange("children", old_children,
					children);
		}

		public void setOpen(boolean open) {
			boolean old_open = this.open;
			this.open = open;
			propertyChangeSupport().firePropertyChange("open", old_open, open);
			if (open) {
				ensureChildren();
			}
		}

		protected void ensureChildren() {
			if (this.children == null) {
				setChildren(new ArrayList<>());
			}
		}

		public static class LabelClicked extends NodeTopic {
		}

		@Directed(tag = "label")
		public static class NodeLabel extends Model {
			private Object toggle = new Object();

			private Object label = "";

			@Directed(tag = "label", behaviours = @Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = @TopicBehaviour(topic = LabelClicked.class, type = TopicBehaviour.TopicBehaviourType.EMIT)))
			public Object getLabel() {
				return this.label;
			}

			@Directed(tag = "span", behaviours = @Behaviour(handler = EmitTopicHandler.class, event = DomEvents.Click.class, topics = @TopicBehaviour(topic = ToggleButtonClicked.class, type = TopicBehaviour.TopicBehaviourType.EMIT)))
			public Object getToggle() {
				return this.toggle;
			}

			public void setLabel(Object label) {
				Object old_label = this.label;
				this.label = label;
				propertyChangeSupport().firePropertyChange("label", old_label,
						label);
			}
		}

		public static class ToggleButtonClicked extends NodeTopic {
		}
	}
}
