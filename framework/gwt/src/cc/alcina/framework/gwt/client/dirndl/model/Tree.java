package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.IntersectionObserved;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.model.Tree.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.Tree.TreeNode;
import cc.alcina.framework.gwt.client.dirndl.model.TreeEvents.KeyboardSelectNode;
import cc.alcina.framework.gwt.client.dirndl.model.TreeEvents.NodeLabelClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeEvents.NodeToggleButtonClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TreeEvents.PaginatorVisible;
import cc.alcina.framework.gwt.client.dirndl.model.TreeEvents.SelectNode;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath.Walker;

@Directed(
	className = "tree",
	bindings = {
			// receive keyevents when focussed
			@Binding(to = "tabIndex", literal = "-1", type = Type.PROPERTY) },
	emits = SelectionChanged.class)
public class Tree<TN extends TreeNode<TN>> extends Model
		implements NodeLabelClicked.Handler, NodeToggleButtonClicked.Handler,
		TreeEvents.SelectNode.Handler, TreeEvents.KeyboardSelectNode.Handler,
		PaginatorVisible.Handler, KeyboardNavigation.Navigation.Handler,
		// routes keydown events to the keyboardNavigation and
		DomEvents.KeyDown.Handler {
	private boolean rootHidden;

	private TN root;

	protected TN selectedNodeModel;

	protected TN keyboardSelectedNodeModel;

	private Paginator paginator;

	KeyboardNavigation keyboardNavigation;

	boolean commitAfterKeyboardNavigation;

	public void attachKeyboardNavigation() {
		keyboardNavigation = new KeyboardNavigation(this)
				.withEmitLeftRightEvents(true);
	}

	@Directed
	public Paginator getPaginator() {
		return this.paginator;
	}

	@Directed
	public TN getRoot() {
		return this.root;
	}

	public boolean isCommitAfterKeyboardNavigation() {
		return this.commitAfterKeyboardNavigation;
	}

	@Binding(type = Type.CSS_CLASS)
	public boolean isRootHidden() {
		return this.rootHidden;
	}

	@Override
	public void onKeyboardSelectNode(KeyboardSelectNode event) {
		keyboardSelectModel((ModelEvent) event);
	}

	@Override
	public void onKeyDown(KeyDown event) {
		if (keyboardNavigation != null) {
			keyboardNavigation.onKeyDown(event);
		}
	}

	@Override
	public void onNavigation(Navigation event) {
		if (keyboardSelectedNodeModel != null) {
			if (keyboardSelectedNodeModel instanceof KeyboardNavigation.Navigation.Handler) {
				((KeyboardNavigation.Navigation.Handler) keyboardSelectedNodeModel)
						.onNavigation(event);
			}
		}
	}

	@Override
	public void onNodeLabelClicked(NodeLabelClicked event) {
		focusTree();
		selectNode((ModelEvent) event);
	}

	@Override
	public void onNodeToggleButtonClicked(NodeToggleButtonClicked event) {
		focusTree();
		TN model = event.getModel();
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

	@Override
	public void onSelectNode(SelectNode event) {
		selectNode((ModelEvent) event);
	}

	public void setCommitAfterKeyboardNavigation(
			boolean commitAfterKeyboardNavigation) {
		this.commitAfterKeyboardNavigation = commitAfterKeyboardNavigation;
	}

	public void setPaginator(Paginator paginator) {
		Paginator old_paginator = this.paginator;
		this.paginator = paginator;
		propertyChangeSupport().firePropertyChange("paginator", old_paginator,
				paginator);
	}

	public void setRoot(TN root) {
		TN old_root = this.root;
		this.root = root;
		propertyChangeSupport().firePropertyChange("root", old_root, root);
	}

	public void setRootHidden(boolean rootHidden) {
		this.rootHidden = rootHidden;
	}

	protected void loadChildren(TN model) {
	}

	protected void loadNextPage() {
		throw new UnsupportedOperationException();
	}

	void focusTree() {
		provideElement().focus();
	}

	void keyboardSelectModel(ModelEvent<TN, ?> event) {
		TN model = event.getModel();
		if (model == keyboardSelectedNodeModel) {
			return;
		}
		if (keyboardSelectedNodeModel != null) {
			keyboardSelectedNodeModel.setKeyboardSelected(false);
		}
		if (selectedNodeModel != null) {
			selectedNodeModel.setSelected(false);
		}
		keyboardSelectedNodeModel = model;
		keyboardSelectedNodeModel.setKeyboardSelected(true);
		if (commitAfterKeyboardNavigation) {
			selectNode(event);
		}
	}

	void selectNode(ModelEvent<TN, ?> event) {
		keyboardSelectModel(event);
		TN model = event.getModel();
		if (selectedNodeModel != null) {
			selectedNodeModel.setSelected(false);
		}
		selectedNodeModel = model;
		selectedNodeModel.setSelected(true);
		event.reemitAs(this, SelectionChanged.class, model);
	}

	/**
	 * Note that subclasses should *not* call the no-args constructor
	 *
	 *
	 *
	 * @param <PN>
	 */
	/*
	 * FIXME - dirndl 1x1dy - this should expect a relative, not absolute path -
	 * at the moment it expects an absolute path, which is just 'get path from
	 * parent'
	 *
	 */
	public abstract static class AbstractPathNode<PN extends AbstractPathNode>
			extends TreeNode<PN>
			implements KeyboardNavigation.Navigation.Handler {
		protected TreePath<PN> treePath;

		/**
		 * Should not be called by child constructor - but required for
		 * serialization
		 */
		public AbstractPathNode() {
		}

		public AbstractPathNode(PN parent, String path) {
			this(parent, path, true);
		}

		public AbstractPathNode(PN parent, String path,
				boolean addToParentChildren) {
			setParent(parent);
			if (parent == null) {
				treePath = TreePath.absolutePath(path);
			} else {
				treePath = parent.treePath.ensurePath(path);
				if (addToParentChildren) {
					parent.getChildren().add(this);
				}
			}
			treePath.setValue((PN) this);
		}

		public TreePath<PN> getTreePath() {
			return this.treePath;
		}

		@Override
		public void onNavigation(Navigation event) {
			TreePath keyboardSelect = null;
			Walker<PN> walker = treePath.walker();
			switch (event.getModel()) {
			case COMMIT:
				event.reemitAs(this, TreeEvents.SelectNode.class, this);
				break;
			case RIGHT:
				if (!isOpen()) {
					setOpen(true);
				} else {
					keyboardSelect = walker.next();
				}
				break;
			case LEFT:
				if (isOpen()) {
					setOpen(false);
				} else {
					keyboardSelect = treePath.getParent();
				}
				break;
			case UP:
				while (walker.previous() != null) {
					if (walker.current().provideIsVisible()) {
						keyboardSelect = walker.current;
						break;
					}
				}
				break;
			case DOWN:
				while (walker.next() != null) {
					if (walker.current().provideIsVisible()) {
						keyboardSelect = walker.current;
						break;
					}
				}
				break;
			}
			if (keyboardSelect != null) {
				event.reemitAs(this, TreeEvents.KeyboardSelectNode.class,
						keyboardSelect.getValue());
			}
		}

		public boolean provideIsVisible() {
			PN parent = getParent();
			if (parent == null) {
				return true;
			} else {
				return parent.isOpen();
			}
		}

		public void putTree(Tree tree) {
			getTreePath().putTree(tree);
		}
	}

	public static class LabelClicked
			extends ModelEvent<Object, LabelClicked.Handler> {
		@Override
		public void dispatch(LabelClicked.Handler handler) {
			handler.onLabelClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onLabelClicked(LabelClicked LabelClicked);
		}
	}

	@Directed(
		tag = "paginator",
		bindings = @Binding(type = Type.INNER_TEXT, from = "text"))
	public static class Paginator extends Model
			implements InferredDomEvents.IntersectionObserved.Handler {
		private String text;

		private boolean fired;

		public String getText() {
			return this.text;
		}

		@Override
		public void onBind(Bind event) {
			int debug = 3;
			super.onBind(event);
		}

		@Override
		public void onIntersectionObserved(IntersectionObserved event) {
			if (event.isIntersecting() && !fired) {
				fired = true;
				Context context = NodeEvent.Context
						.fromContext(event.getContext(), null);
				context.dispatch(PaginatorVisible.class, null);
			}
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class PathNode extends AbstractPathNode<PathNode> {
		public PathNode() {
		}

		public PathNode(PathNode parent, String path) {
			super(parent, path);
		}
	}

	public static class SelectionChanged
			extends ModelEvent<TreeNode, SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	public static class ToggleButtonClicked
			extends ModelEvent<Object, ToggleButtonClicked.Handler> {
		@Override
		public void dispatch(ToggleButtonClicked.Handler handler) {
			handler.onToggleButtonClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onToggleButtonClicked(ToggleButtonClicked event);
		}
	}

	@Directed(
		className = "node",
		reemits = { LabelClicked.class, NodeLabelClicked.class,
				ToggleButtonClicked.class, NodeToggleButtonClicked.class })
	public static class TreeNode<NM extends TreeNode> extends Model {
		public transient boolean populated;

		private boolean open;

		private NodeLabel label = new NodeLabel();

		private List<TreeNode<NM>> children = new IdentityArrayList<>();

		private NM parent;

		private boolean leaf;

		private boolean selected;

		private boolean keyboardSelected;

		@Directed.Wrap("nodes")
		public List<TreeNode<NM>> getChildren() {
			return this.children;
		}

		@Directed
		public NodeLabel getLabel() {
			return this.label;
		}

		public NM getParent() {
			return this.parent;
		}

		@Binding(type = Type.CSS_CLASS)
		public boolean isKeyboardSelected() {
			return this.keyboardSelected;
		}

		@Binding(type = Type.CSS_CLASS)
		public boolean isLeaf() {
			return this.leaf;
		}

		@Binding(type = Type.CSS_CLASS)
		public boolean isOpen() {
			return this.open;
		}

		@Binding(type = Type.CSS_CLASS)
		public boolean isSelected() {
			return this.selected;
		}

		// FIXME - dirndl 1x1g - classic incremental collection modification,
		// have ChildReplacer (DirectedLayout) preserve the list (change the
		// model, keep the node) and just handle the element delta
		//
		// also - directedlayout buffer changes - child replacer doesn't fire
		// immediately, rather adds to queue (although I think that's required
		// by the reentrancy logic anyway - at least if there's an active layout
		// pass)
		public void setChildren(List<TreeNode<NM>> children) {
			List<TreeNode<NM>> old_children = this.children;
			this.children = children;
			propertyChangeSupport().firePropertyChange("children", old_children,
					children);
		}

		public void setKeyboardSelected(boolean keyboardSelected) {
			set("keyboardSelected", this.keyboardSelected, keyboardSelected,
					() -> this.keyboardSelected = keyboardSelected);
		}

		public void setLeaf(boolean leaf) {
			this.leaf = leaf;
		}

		public void setOpen(boolean open) {
			set("open", this.open, open, () -> this.open = open);
		}

		public void setParent(NM parent) {
			this.parent = parent;
		}

		public void setSelected(boolean selected) {
			set("selected", this.selected, selected,
					() -> this.selected = selected);
		}

		public void sortChildren() {
			IdentityArrayList<TreeNode<NM>> sorted = IdentityArrayList
					.copyOf(children);
			Collections.sort((List<? extends Comparable>) (List<?>) sorted);
			setChildren(sorted);
		}

		@Directed(
			tag = "label",
			bindings = { @Binding(
				from = "title",
				to = "title",
				type = Binding.Type.PROPERTY) })
		@TypeSerialization(reflectiveSerializable = false)
		public static class NodeLabel extends Model {
			private Object toggle = new Object();

			private Object label = "";

			private String title;

			@Directed(
				merge = true,
				reemits = { DomEvents.Click.class, LabelClicked.class })
			public Object getLabel() {
				return this.label;
			}

			public String getTitle() {
				return this.title;
			}

			@Directed(
				tag = "span",
				reemits = { DomEvents.Click.class, ToggleButtonClicked.class })
			public Object getToggle() {
				return this.toggle;
			}

			public void setLabel(Object label) {
				set("label", this.label, label, () -> this.label = label);
			}

			public void setTitle(String title) {
				set("title", this.title, title, () -> this.title = title);
			}
		}

		@Directed(
			tag = "label",
			bindings = { @Binding(from = "text", type = Type.INNER_TEXT),
					@Binding(
						from = "text",
						to = "title",
						type = Type.PROPERTY) })
		public static class NodeLabelText extends Model {
			private String text;

			public NodeLabelText() {
			}

			public NodeLabelText(String text) {
				this.text = text;
			}

			public String getText() {
				return this.text;
			}

			public void setText(String text) {
				this.text = text;
			}
		}
	}
}
