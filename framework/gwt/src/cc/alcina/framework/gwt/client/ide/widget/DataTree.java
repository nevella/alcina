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
package cc.alcina.framework.gwt.client.ide.widget;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventEvent;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventListener;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventSource;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventSupport;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventType;
import cc.alcina.framework.gwt.client.widget.TreeNodeWalker;
import cc.alcina.framework.gwt.client.widget.VisualFilterable.VisualFilterableWithFirst;

/**
 * 
 * @author Nick Reddel
 */
public class DataTree extends FilterableTree
		implements ExtraTreeEventSource, VisualFilterableWithFirst {
	public static final String DEBUG_ID = "DataTree";

	private ExtraTreeEventSupport extraTreeEventSupport;

	public void addExtraTreeEventListener(ExtraTreeEventListener listener) {
		this.extraTreeEventSupport.addExtraTreeEventListener(listener);
	}

	public void fireActionsAvailbleChange(ExtraTreeEventEvent event) {
		this.extraTreeEventSupport.fireActionsAvailbleChange(event);
	}

	public void removeExtraTreeEventListener(ExtraTreeEventListener listener) {
		this.extraTreeEventSupport.removeExtraTreeEventListener(listener);
	}

	public DataTree() {
		super();
		sinkEvents(Event.ONDBLCLICK);
		sinkEvents(Event.ONCONTEXTMENU);
		this.extraTreeEventSupport = new ExtraTreeEventSupport();
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		for (int i = 0; i < getItemCount(); i++) {
			TreeItem child = getItem(i);
			if (child instanceof DetachListener)
				((DetachListener) child).onDetach();
		}
	}

	@Override
	public void onBrowserEvent(Event event) {
		int eventType = DOM.eventGetType(event);
		super.onBrowserEvent(event);// allow selection
		if (getSelectedItem() != null) {
			switch (eventType) {
			case Event.ONDBLCLICK:
				if (getSelectedItem() != null) {
					event.preventDefault();
					fireActionsAvailbleChange(new ExtraTreeEventEvent(
							getSelectedItem(), ExtraTreeEventType.DBL_CLICK));
				}
			case Event.ONCONTEXTMENU:
				event.preventDefault();
				fireActionsAvailbleChange(new ExtraTreeEventEvent(
						getSelectedItem(), ExtraTreeEventType.RIGHT_CLICK));
			case Event.ONKEYUP:
				if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
					fireActionsAvailbleChange(new ExtraTreeEventEvent(
							getSelectedItem(), ExtraTreeEventType.DBL_CLICK));
				}
			}
		}
	}

	private TreeItem result;

	protected boolean initialised;

	public TreeItem selectNodeForObject(Object obj) {
		getNodeForObject(obj);
		if (result != null) {
			setSelectedItem(null);
			setSelectedItem(result);
			ensureSelectedItemVisible();
		}
		return result;
	}

	public TreeItem getNodeForObject(final Object obj) {
		result = null;
		final boolean classNameTest = (obj instanceof String);
		Callback<TreeItem> callback = new Callback<TreeItem>() {
			public void apply(TreeItem target) {
				Object userObject = target.getUserObject();
				if (userObject != null) {
					if ((classNameTest && userObject.getClass().getName()
							.replace("$", ".").equals(obj))
							|| obj.equals(userObject)) {
						result = target;
					}
				}
			}
		};
		new TreeNodeWalker().walk(this, callback);
		return result;
	}
}
