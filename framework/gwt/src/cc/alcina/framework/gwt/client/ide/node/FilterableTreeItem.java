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
package cc.alcina.framework.gwt.client.ide.node;

import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.gwt.client.widget.VisualFilterable.VisualFilterableWithParentEnforcesChildVisibility;

/**
 * 
 * @author Nick Reddel
 */
public class FilterableTreeItem extends TreeItem implements
		VisualFilterableWithParentEnforcesChildVisibility, NodeFactoryProvider {
	private NodeFactory nodeFactory;

	public FilterableTreeItem() {
	}

	public FilterableTreeItem(NodeFactory nodeFactory) {
		this.nodeFactory = nodeFactory;
	}

	public boolean filter(String filterText) {
		return filter(filterText, false);
	}

	public boolean filter(String filterText, boolean enforceVisible) {
		boolean satisfiesFilter = satisfiesFilter(filterText);
		boolean satisfiesFilterThisNode = satisfiesFilter;
		for (int i = 0; i < getChildCount(); i++) {
			TreeItem child = getChild(i);
			if (child instanceof VisualFilterableWithParentEnforcesChildVisibility) {
				VisualFilterableWithParentEnforcesChildVisibility vf = (VisualFilterableWithParentEnforcesChildVisibility) child;
				satisfiesFilterThisNode |= vf.filter(filterText,
						satisfiesFilter | enforceVisible);
			}
		}
		satisfiesFilterThisNode |= getText().toLowerCase().contains(filterText);
		setVisible(satisfiesFilterThisNode || enforceVisible);
		if (satisfiesFilterThisNode && filterText != "") {
			setState(true, false);
		}
		return satisfiesFilterThisNode;
	}

	public NodeFactory getNodeFactory() {
		return this.nodeFactory;
	}

	@Override
	public String getText() {
		String text = super.getText();
		return text == null ? "" : text;
	}

	protected boolean satisfiesFilter(String filterText) {
		return getText().toLowerCase().contains(filterText);
	}
}
