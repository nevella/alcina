package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focus;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.IntersectionObserved;
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
				if (!path.contains(".")) {
					path = parent.treePath.childPath(path);
				}
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
			boolean exitUp = false;
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
				if (keyboardSelect == null) {
					exitUp = true;
				} else if (Objects.equals(keyboardSelect, treePath)) {
					exitUp = true;
				} else {
					Tree tree = (Tree) keyboardSelect.provideContainingTree();
					if (Objects.equals(keyboardSelect,
							((AbstractPathNode) tree.root).treePath)
							&& tree.rootHidden) {
						exitUp = true;
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
			case EXIT_UP:
				event.bubble();
				break;
			}
			if (keyboardSelect != null) {
				event.reemitAs(this, TreeEvents.KeyboardSelectNode.class,
						keyboardSelect.getValue());
			}
			if (exitUp) {
				event.reemitAs(this, KeyboardNavigation.Navigation.class,
						KeyboardNavigation.Navigation.Type.EXIT_UP);
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
		public interface Handler extends NodeEvent.Handler {
			void onLabelClicked(LabelClicked LabelClicked);
		}

		@Override
		public void dispatch(LabelClicked.Handler handler) {
			handler.onLabelClicked(this);
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
		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}

		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}
	}

	public static class ToggleButtonClicked
			extends ModelEvent<Object, ToggleButtonClicked.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onToggleButtonClicked(ToggleButtonClicked event);
		}

		@Override
		public void dispatch(ToggleButtonClicked.Handler handler) {
			handler.onToggleButtonClicked(this);
		}
	}

	public static class ToggleButtonMouseDown
			extends ModelEvent<Object, ToggleButtonMouseDown.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onToggleButtonMousedown(ToggleButtonMouseDown event);
		}

		@Override
		public void dispatch(ToggleButtonMouseDown.Handler handler) {
			handler.onToggleButtonMousedown(this);
		}
	}

	/**
	 * Note that the code almost always refers to NM (the concrete type of the
	 * node in the tree), rather than TreeNod<NM>
	 */
	@Directed(
		className = "node",
		reemits = { LabelClicked.class, NodeLabelClicked.class,
				ToggleButtonClicked.class, NodeToggleButtonClicked.class })
	public abstract static class TreeNode<NM extends TreeNode> extends Model
			implements DomEvents.Focus.Handler {
		/**
		 * Use this for simple/demo trees
		 */
		public static class BasicNode extends TreeNode<BasicNode> {
			public BasicNode(BasicNode parent, String label) {
				super(parent, label);
			}
		}

		@TypeSerialization(reflectiveSerializable = false)
		@Directed(tag = "node-label")
		public static class NodeLabel extends Model.All
				implements ToggleButtonMouseDown.Handler {
			@Directed(
				tag = "span",
				reemits = { DomEvents.Click.class, ToggleButtonClicked.class,
						DomEvents.MouseDown.class,
						ToggleButtonMouseDown.class })
			public Object toggle = new Object();

			@Directed(
				merge = true,
				reemits = { DomEvents.Click.class, LabelClicked.class })
			public Object label = "";

			@Binding(type = Type.PROPERTY)
			public String title;

			@Override
			public void onToggleButtonMousedown(ToggleButtonMouseDown event) {
				event.getContext().getOriginatingNativeEvent().preventDefault();
			}
		}

		@Directed(tag = "node-label")
		public static class NodeLabelText extends Model.Fields {
			@Binding(type = Type.INNER_TEXT)
			public String text;

			@Binding(type = Type.PROPERTY)
			public String title;

			@Binding(type = Type.CLASS_PROPERTY)
			public String className;

			public NodeLabelText() {
			}

			public NodeLabelText(String text) {
				this(text, null);
			}

			public NodeLabelText(String text, String className) {
				putText(text);
				this.className = className;
			}

			public void putText(String text) {
				this.text = text;
				this.title = text;
			}
		}

		public transient boolean populated;

		private boolean open;

		private NodeLabel label = new NodeLabel();

		private List<NM> children = new IdentityArrayList<>();

		private NM parent;

		private boolean leaf;

		private boolean selected;

		private boolean keyboardSelected;

		public TreeNode() {
		}

		public TreeNode(NM parent, String label) {
			addTo(parent);
			this.label.label = label;
		}

		public Stream<NM> stream(boolean includeSelf) {
			Function<NM, List<NM>> childSupplier = n -> (List) n.getChildren();
			DepthFirstTraversal<NM> traversal = new DepthFirstTraversal<>(
					(NM) this, childSupplier);
			return traversal.stream().skip(includeSelf ? 0 : 1);
		}

		public int depth() {
			int depth = 0;
			TreeNode cursor = this;
			while (cursor.getParent() != null) {
				depth++;
				cursor = cursor.getParent();
			}
			return depth;
		}

		protected NM addTo(TreeNode<? extends NM> genericParent) {
			NM parent = (NM) genericParent;
			if (parent != null) {
				parent.getChildren().add(this);
				this.parent = parent;
			}
			return (NM) this;
		}

		@Directed.Wrap("nodes")
		public List<NM> getChildren() {
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
		public void setChildren(List<NM> children) {
			List<NM> old_children = this.children;
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
			IdentityArrayList<NM> sorted = IdentityArrayList.copyOf(children);
			Collections.sort((List<? extends Comparable>) (List<?>) sorted);
			setChildren(sorted);
		}

		public void openTo() {
			TreeNode cursor = this;
			while (cursor != null) {
				if (!cursor.isLeaf()) {
					cursor.setOpen(true);
				}
				cursor = cursor.getParent();
			}
		}

		@Override
		public void onFocus(Focus event) {
			event.reemitAs(this, KeyboardSelectNode.class, this);
		}
	}

	private boolean rootHidden;

	private TN root;

	protected TN selectedNodeModel;

	protected TN keyboardSelectedNodeModel;

	private Paginator paginator;

	KeyboardNavigation keyboardNavigation;

	boolean commitAfterKeyboardNavigation;

	/**
	 * If true, repeated selection (clicks etc) of a node toggle selection
	 */
	public boolean selectionToggle;

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
		onKeyboardSelectModelEvent((ModelEvent) event);
	}

	@Override
	public void onKeyDown(KeyDown event) {
		if (keyboardNavigation != null) {
			keyboardNavigation.onKeyDown(event);
		}
	}

	@Override
	public void onNavigation(Navigation event) {
		if (event.getModel() == Navigation.Type.EXIT_UP) {
			event.bubble();
			return;
		}
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
		onSelectEvent((ModelEvent) event);
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
		onSelectEvent((ModelEvent) event);
	}

	public void selectNode(TN node) {
		keyboardSelectTreeNode(node);
		selectNode0(node);
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

	public void focusForKeyboardNavigation() {
		focusTree();
		TreeNode<TN> toFocus = Ax.first(root.getChildren());
		if (toFocus != null) {
			emitEvent(TreeEvents.SelectNode.class, toFocus);
		}
	}

	protected void loadChildren(TN model) {
	}

	protected void loadNextPage() {
		throw new UnsupportedOperationException();
	}

	void focusTree() {
		provideElement().focus();
	}

	void onKeyboardSelectModelEvent(ModelEvent<TN, ?> event) {
		TN node = event.getModel();
		if (node == keyboardSelectedNodeModel) {
			return;
		}
		keyboardSelectTreeNode(node);
		if (commitAfterKeyboardNavigation) {
			onSelectEvent(event);
		}
	}

	void keyboardSelectTreeNode(TN node) {
		if (node == keyboardSelectedNodeModel) {
			return;
		}
		if (keyboardSelectedNodeModel != null) {
			keyboardSelectedNodeModel.setKeyboardSelected(false);
		}
		if (selectedNodeModel != null) {
			selectedNodeModel.setSelected(false);
		}
		keyboardSelectedNodeModel = node;
		keyboardSelectedNodeModel.setKeyboardSelected(true);
	}

	LastEvent lastEvent;

	/*
	 * Prevent double-firing due to focus/click
	 */
	class LastEvent {
		ModelEvent<TN, ?> event;

		long time;

		LastEvent(ModelEvent<TN, ?> event) {
			this.event = event;
			this.time = System.currentTimeMillis();
		}

		boolean isRecastBy(ModelEvent<TN, ?> event) {
			if (event.getModel() == this.event.getModel()) {
				if (TimeConstants.within(time, GWT.isProdMode() ? 200 : 2000)) {
					return true;
				}
			}
			return false;
		}
	}

	void onSelectEvent(ModelEvent<TN, ?> event) {
		if (lastEvent != null && lastEvent.isRecastBy(event)) {
			return;
		}
		lastEvent = new LastEvent(event);
		onKeyboardSelectModelEvent(event);
		TN node = event.getModel();
		TN selectedNode = selectNode0(node);
		event.reemitAs(this, SelectionChanged.class, selectedNode);
	}

	TN selectNode0(TN node) {
		if (selectedNodeModel != null) {
			selectedNodeModel.setSelected(false);
		}
		if (selectedNodeModel == node && selectionToggle) {
			return null;
		} else {
			selectedNodeModel = node;
			selectedNodeModel.setSelected(true);
			return node;
		}
	}
}
