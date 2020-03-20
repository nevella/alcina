/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.widget;

import java.util.Stack;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.util.Callback;


/**
 *
 * @author Nick Reddel
 */
public class TreeNodeWalker {
	private boolean cancelled;

	protected void cancel() {
		this.cancelled = true;
	}
	public abstract static class TreeNodeWalkerCallback implements Callback<TreeItem>{
		protected TreeNodeWalker walker;
		protected void cancel() {
			walker.cancel();
		}
	}

	public void walk(Tree tree, Callback callback) {
		Stack<TreeItem> items = new Stack<TreeItem>();
		int itemCount = tree.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			items.push(tree.getItem(i));
		}
		walk(items,callback);
		
	}

	private void walk(Stack<TreeItem> items, Callback callback) {
		if(callback instanceof TreeNodeWalkerCallback) {
			((TreeNodeWalkerCallback) callback).walker=this;
			
		}
		while (!items.isEmpty()) {
			if(cancelled) {
				return ;
			}
			TreeItem pop = items.pop();
			callback.apply(pop);
			for (int i = 0; i < pop.getChildCount(); i++) {
				items.push(pop.getChild(i));
			}
		}		
	}

	public void walk(TreeItem item, Callback callback) {
		Stack<TreeItem> items = new Stack<TreeItem>();
		items.push(item);
		walk(items,callback);
	}
}