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


import cc.alcina.framework.gwt.client.ide.widget.DetachListener;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ContainerNode extends FilterableTreeItem implements DetachListener {
	private final String title;

	private final AbstractImagePrototype imagePrototype;

	public ContainerNode(String title, ImageResource imageResource) {
		super();
		this.title = title;
		this.imagePrototype = AbstractImagePrototype.create(imageResource);
		setHTML(imageItemHTML(imagePrototype, title));
	}

	public AbstractImagePrototype getImagePrototype() {
		return this.imagePrototype;
	}

	public String getTitle() {
		return this.title;
	}

	protected String imageItemHTML(AbstractImagePrototype imageProto,
			String title) {
		return imageProto.getHTML() + " " + title;
	}

	public void onDetach() {
		for (int i = 0; i < getChildCount(); i++) {
			TreeItem child = getChild(i);
			if (child instanceof DetachListener)
				((DetachListener) child).onDetach();
		}
	}
}
