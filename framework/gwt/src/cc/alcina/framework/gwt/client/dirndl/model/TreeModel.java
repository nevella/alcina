package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.IntersectionObserved;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.layout.CollectionNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererArgs;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererLeaf;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.NodeModel;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModel.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModelEvents.NodeLabelClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModelEvents.NodeToggleButtonClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeModelEvents.PaginatorVisible;

@Directed(tag = "div", cssClass = "dl-tree", bindings = {
		@Binding(from = "hideRoot", type = Type.CSS_CLASS) }, receives = {
				TreeModelEvents.NodeLabelClicked.class,
				TreeModelEvents.NodeToggleButtonClicked.class,
				TreeModelEvents.PaginatorVisible.class }, emits = SelectionChanged.class)
public class TreeModel<NM extends NodeModel<NM>> extends Model
		implements NodeLabelClicked.Handler, NodeToggleButtonClicked.Handler,
		PaginatorVisible.Handler {
	private boolean hideRoot;

	private NM root;

	protected NM selectedNodeModel;

	private Paginator paginator;

	@Directed
	public Paginator getPaginator() {
		return this.paginator;
	}

	@Directed
	public NM getRoot() {
		return this.root;
	}

	public boolean isHideRoot() {
		return this.hideRoot;
	}

	@Override
	public void onNodeLabelClicked(NodeLabelClicked event) {
		NM model = event.getModel();
		if (selectedNodeModel != null) {
			selectedNodeModel.setSelected(false);
		}
		selectedNodeModel = model;
		selectedNodeModel.setSelected(true);
		Context context = NodeEvent.Context.newTopicContext(event.getContext(),
				null);
		TopicEvent.fire(context, SelectionChanged.class, model);
	}

	@Override
	public void onNodeToggleButtonClicked(NodeToggleButtonClicked event) {
		NM model = event.getModel();
		model.setOpen(!model.isOpen());
		if (model.isOpen() && !model.populated) {
			model.populated = true;
			loadChildren(model);
		}
	}

	@Override
	public void onPaginatorVisible(PaginatorVisible event) {
		throw new UnsupportedOperationException();
	}

	public void setHideRoot(boolean hideRoot) {
		this.hideRoot = hideRoot;
	}

	public void setPaginator(Paginator paginator) {
		Paginator old_paginator = this.paginator;
		this.paginator = paginator;
		propertyChangeSupport().firePropertyChange("paginator", old_paginator,
				paginator);
	}

	public void setRoot(NM root) {
		NM old_root = this.root;
		this.root = root;
		propertyChangeSupport().firePropertyChange("root", old_root, root);
	}

	protected void loadChildren(NM model) {
	}

	protected void loadNextPage() {
		throw new UnsupportedOperationException();
	}

	public static class ChildrenLoaded extends NodeTopic {
	}

	public static class LabelClicked
			extends TopicEvent<Object, LabelClicked.Handler> {
		@Override
		public void dispatch(LabelClicked.Handler handler) {
			handler.onLabelClicked(this);
		}

		@Override
		public Class<LabelClicked.Handler> getHandlerClass() {
			return LabelClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onLabelClicked(LabelClicked LabelClicked);
		}
	}

	@Directed(tag = "div", cssClass = "dl-tree-node", bindings = {
			@Binding(from = "open", type = Type.CSS_CLASS, literal = "open"),
			@Binding(from = "selected", type = Type.CSS_CLASS, literal = "selected"),
			@Binding(from = "leaf", type = Type.CSS_CLASS, literal = "leaf") }, receives = {
					LabelClicked.class, ToggleButtonClicked.class }, reemits = {
							NodeLabelClicked.class,
							NodeToggleButtonClicked.class })
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

		@Directed(tag = "label", bindings = {
				@Binding(from = "title", to = "title", type = Binding.Type.PROPERTY) })
		public static class NodeLabel extends Model {
			private Object toggle = new Object();

			private Object label = "";

			private String title;

			@Directed(merge = true, receives = DomEvents.Click.class, reemits = LabelClicked.class)
			public Object getLabel() {
				return this.label;
			}

			public String getTitle() {
				return this.title;
			}

			@Directed(tag = "span", receives = DomEvents.Click.class, reemits = ToggleButtonClicked.class)
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

		@Directed(tag = "label", bindings = @Binding(from = "text", type = Type.INNER_TEXT))
		public static class NodeLabelText extends Model {
			private String text;

			public String getText() {
				return this.text;
			}

			public void setText(String text) {
				this.text = text;
			}
		}
	}

	@Directed(tag = "paginator", bindings = @Binding(type = Type.INNER_TEXT, from = "text"), receives = InferredDomEvents.IntersectionObserved.class)
	public static class Paginator extends Model
			implements InferredDomEvents.IntersectionObserved.Handler {
		private String text;

		private boolean fired;

		public String getText() {
			return this.text;
		}

		@Override
		public void onIntersectionObserved(IntersectionObserved event) {
			if (event.isIntersecting() && !fired) {
				fired = true;
				Context context = NodeEvent.Context
						.newTopicContext(event.getContext(), null);
				TopicEvent.fire(context, PaginatorVisible.class, null);
			}
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class SelectionChanged
			extends TopicEvent<NodeModel, SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		@Override
		public Class<SelectionChanged.Handler> getHandlerClass() {
			return SelectionChanged.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	public static class ToggleButtonClicked
			extends TopicEvent<Object, ToggleButtonClicked.Handler> {
		@Override
		public void dispatch(ToggleButtonClicked.Handler handler) {
			handler.onToggleButtonClicked(this);
		}

		@Override
		public Class<ToggleButtonClicked.Handler> getHandlerClass() {
			return ToggleButtonClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onToggleButtonClicked(ToggleButtonClicked event);
		}
	}
}
