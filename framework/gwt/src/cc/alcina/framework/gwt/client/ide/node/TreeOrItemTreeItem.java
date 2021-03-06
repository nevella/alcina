package cc.alcina.framework.gwt.client.ide.node;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class TreeOrItemTreeItem implements TreeOrItem {
	private final TreeItem item;

	public TreeOrItemTreeItem(TreeItem item) {
		this.item = item;
	}

	public void addItem(TreeItem item) {
		this.item.addItem(item);
	}

	public TreeItem getChild(int index) {
		return this.item.getChild(index);
	}

	public int getChildCount() {
		return this.item.getChildCount();
	}

	@Override
	public int getChildIndex(TreeItem child) {
		return item.getChildIndex(child);
	}

	@Override
	public NodeFactory getNodeFactory() {
		if (item instanceof NodeFactoryProvider) {
			return ((NodeFactoryProvider) item).getNodeFactory();
		}
		return null;
	}

	@Override
	public TreeOrItem getParent() {
		return TreeOrItemTree
				.create(item.getParentItem() != null ? item.getParentItem()
						: item.getTree());
	}

	@Override
	public boolean getState() {
		return item.getState();
	}

	public Tree getTree() {
		return item.getTree();
	}

	public void removeItem(TreeItem item) {
		this.item.removeItem(item);
	}

	public void removeItems() {
		this.item.removeItems();
	}
}