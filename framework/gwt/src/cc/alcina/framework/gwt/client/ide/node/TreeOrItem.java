package cc.alcina.framework.gwt.client.ide.node;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public interface TreeOrItem {
	public void addItem(TreeItem item);

	public void removeItem(TreeItem item);

	public void removeItems();

	public TreeItem getChild(int index);

	public int getChildCount();

	public Tree getTree();

	public boolean getState();

	public NodeFactory getNodeFactory();
	
	public TreeOrItem getParent();

	public int getChildIndex(TreeItem item);
	
	
}