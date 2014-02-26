package cc.alcina.framework.gwt.client.ide.node;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class TreeOrItemTree implements TreeOrItem {
	private final Tree tree;

	public TreeItem getChild(int index) {
		return this.tree.getItem(index);
	}

	public int getChildCount() {
		return this.tree.getItemCount();
	}

	public void addItem(TreeItem item) {
		this.tree.addItem(item);
	}

	public void removeItem(TreeItem item) {
		this.tree.removeItem(item);
	}

	public void removeItems() {
		this.tree.removeItems();
	}

	public TreeOrItemTree(Tree tree) {
		this.tree = tree;
	}

	public Tree getTree() {
		return tree;
	}

	@Override
	public boolean getState() {
		return true;
	}

	@Override
	public NodeFactory getNodeFactory() {
		if (tree instanceof NodeFactoryProvider) {
			return ((NodeFactoryProvider) tree).getNodeFactory();
		}
		return null;
	}

	public static TreeOrItem create(Object object) {
		if (object instanceof Tree) {
			return new TreeOrItemTree((Tree) object);
		}
		if (object instanceof TreeItem) {
			return new TreeOrItemTreeItem((TreeItem) object);
		}
		return null;
	}

	@Override
	public TreeOrItem getParent() {
		return null;
	}

	@Override
	public int getChildIndex(TreeItem item) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			if (tree.getItem(i) == item) {
				return i;
			}
		}
		return -1;
	}
}