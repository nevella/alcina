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

package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.actions.VetoableAction;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionRegistry;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * Similar to {@link DomainObjectActionLinkCustomiser}, but fires the
 * object/action directly, not via history
 */
@ClientInstantiable
@SuppressWarnings("unchecked")
public class ObjectActionLinkCustomiser implements Customiser {
	public static final String ACTION_CLASS = "actionClass";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		List<VetoableAction> actions = new ArrayList<VetoableAction>();
		for (NamedParameter p : info.parameters()) {
			if (p.name().equals(ACTION_CLASS)) {
				Class c = p.classValue();
				actions.add((VetoableAction) CommonLocator.get()
						.classLookup().newInstance(c));
			}
		}
		return new ObjectActionLinkProvider(actions);
	}

	public static class ObjectActionLink extends AbstractBoundWidget {
		private FlowPanel fp;

		public ObjectActionLink() {
			this.fp = new FlowPanel();
			initWidget(fp);
		}

		public void setValue(Object value) {
			return;
		}

		public Object getValue() {
			return null;
		}

		public void setVetoableActions(List<VetoableAction> vetoableActions) {
			this.vetoableActions = vetoableActions;
			for (VetoableAction a : vetoableActions) {
				Link<VetoableAction> hl = new Link<VetoableAction>(a
						.getDisplayName(), actionCl);
				hl.setWordWrap(false);
				hl.setUserObject(a);
				fp.add(hl);
				fp.add(UsefulWidgetFactory.createSpacer(2));
			}
		}

		private ClickHandler actionCl = new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				VetoableAction a = ((Link<VetoableAction>) sender)
						.getUserObject();
				LooseActionRegistry.get().performForTargetActionAndObject(a,
						getModel());
			}
		};

		public List<VetoableAction> getVetoableActions() {
			return vetoableActions;
		}

		private List<VetoableAction> vetoableActions;
	}

	public static class ObjectActionLinkProvider implements BoundWidgetProvider {
		private final List<VetoableAction> actions;

		public ObjectActionLinkProvider(List<VetoableAction> actions) {
			this.actions = actions;
		}

		public BoundWidget get() {
			ObjectActionLink l = new ObjectActionLink();
			l.setVetoableActions(actions);
			return l;
		}
	}
}