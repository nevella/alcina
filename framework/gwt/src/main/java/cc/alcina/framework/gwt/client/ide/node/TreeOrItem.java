package cc.alcina.framework.gwt.client.ide.node;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public interface TreeOrItem {
	public void addItem(TreeItem item);

	public TreeItem getChild(int index);

	public int getChildCount();

	public int getChildIndex(TreeItem item);

	public NodeFactory getNodeFactory();

	public TreeOrItem getParent();

	public boolean getState();

	public Tree getTree();

	public void removeItem(TreeItem item);

	public void removeItems();
}