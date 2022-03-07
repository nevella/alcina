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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.DataTree;
import cc.alcina.framework.gwt.client.ide.widget.DetachListener;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;

/**
 * @author Nick Reddel
 */
public class DomainNode<T extends SourcesPropertyChangeEvents> extends
		FilterableTreeItem implements PropertyChangeListener, DetachListener {
	private String displayName;

	public DomainNode(T object) {
		this(object, null);
	}

	public DomainNode(T object, NodeFactory nodeFactory) {
		super();
		setUserObject(object);
		object.addPropertyChangeListener(this);
		refreshFromObject();
	}

	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public T getUserObject() {
		return (T) super.getUserObject();
	}

	@Override
	public void onDetach() {
		removeListeners();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (getTree() == null) {
			return;
		}
		refreshFromObject();
	}

	public void refreshFromObject() {
		displayName = TextProvider.get().getObjectName(getUserObject());
		if (displayName != null) {
			displayName = SafeHtmlUtils.htmlEscape(displayName);
		} else {
			displayName = "[null]";
		}
		if (!isUnrendered()) {
			renderHtml();
		}
	}

	@Override
	public void removeItem(TreeItem item) {
		super.removeItem(item);
		removeListeners();
	}

	public void removeListeners() {
		T object = getUserObject();
		object.removePropertyChangeListener(this);
	}

	@Override
	protected String getText0() {
		return displayName;
	}

	protected String imageItemHTML(AbstractImagePrototype imageProto,
			String title) {
		if (((DataTree) getTree()).isUseNodeImages()) {
			return imageProto.getHTML() + " " + title;
		} else {
			return title;
		}
	}

	@Override
	protected void renderHtml() {
		AbstractImagePrototype img = StandardDataImageProvider.get()
				.getByName("");
		setHTML(imageItemHTML(img, displayName));
	}

	@Override
	protected boolean satisfiesFilter(String filterText) {
		T userObject = getUserObject();
		return Registry.query(HasSatisfiesFilter.class)
				.addKeys(userObject.getClass()).impl()
				.satisfiesFilter(userObject, filterText);
	}

	@Reflected
	@Registration.Singleton(HasSatisfiesFilter.class)
	public static class DefaultHasSatisfiesFilter<T>
			implements HasSatisfiesFilter<T> {
		@Override
		public boolean satisfiesFilter(T t, String filterText) {
			if (CommonUtils.nullToEmpty(TextProvider.get().getObjectName(t))
					.toLowerCase().contains(filterText)) {
				return true;
			}
			if (t instanceof HasId) {
				if (filterText.startsWith("id:")) {
					return String.valueOf(((HasId) t).getId())
							.equals(filterText.substring(3));
				}
				return String.valueOf(((HasId) t).getId()).equals(filterText);
			}
			return false;
		}
	}
}
