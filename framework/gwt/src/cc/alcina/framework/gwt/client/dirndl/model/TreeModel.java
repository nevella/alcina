package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.CollectionNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.HtmlNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererArgs;
import cc.alcina.framework.gwt.client.dirndl.layout.MultipleNodeRenderer.MultipleNodeRendererLeaf;

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
		}

		@Override
		@Directed(tag = "div", cssClass = "dl-tree-label", renderer = HtmlNodeRenderer.class)
		public String getHtml() {
			return node.getName();
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
			@Binding(from = "open", type = Type.CSS_CLASS, literal = "open") })
	public static class NodeModel extends Model {
		private boolean open;

		private String html = "";

		protected List<NodeModel> children;

		@Directed(renderer = MultipleNodeRenderer.class)
		@MultipleNodeRendererArgs(tags = { "div" }, cssClasses = {
				"dl-tree-children" })
		@MultipleNodeRendererLeaf(@Directed(renderer = CollectionNodeRenderer.class))
		public List<NodeModel> getChildren() {
			if (!isOpen()) {
				return new ArrayList<>();
			}
			ensureChildren();
			return this.children;
		}

		@Directed(tag = "div", cssClass = "dl-tree-label", renderer = HtmlNodeRenderer.class)
		public String getHtml() {
			return this.html;
		}

		public boolean isOpen() {
			return this.open;
		}

		public void setChildren(List<NodeModel> children) {
			List<NodeModel> old_children = this.children;
			this.children = children;
			propertyChangeSupport().firePropertyChange("children", old_children,
					children);
		}

		public void setHtml(String html) {
			String old_html = this.html;
			this.html = html;
			propertyChangeSupport().firePropertyChange("html", old_html, html);
		}

		public void setOpen(boolean open) {
			this.open = open;
			getChildren();
		}

		protected void ensureChildren() {
			if (this.children == null) {
				setChildren(new ArrayList<>());
			}
		}
	}
}
