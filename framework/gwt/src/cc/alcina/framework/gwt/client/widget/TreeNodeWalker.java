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

import cc.alcina.framework.common.client.util.Callback;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */

 public class TreeNodeWalker {
	public void walk(Tree tree, Callback callback) {
		Stack<TreeItem> items = new Stack<TreeItem>();
		int itemCount = tree.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			items.push(tree.getItem(i));
		}
		while (!items.isEmpty()) {
			TreeItem pop = items.pop();
			callback.callback(pop);
			for (int i = 0; i < pop.getChildCount(); i++) {
				items.push(pop.getChild(i));
			}
		}
	}
	public void walk(TreeItem item, Callback callback) {
		Stack<TreeItem> items = new Stack<TreeItem>();
		items.push(item);
		while (!items.isEmpty()) {
			TreeItem pop = items.pop();
			callback.callback(pop);
			for (int i = 0; i < pop.getChildCount(); i++) {
				items.push(pop.getChild(i));
			}
		}
	}
}