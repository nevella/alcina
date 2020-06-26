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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionRegistry;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

/**
 * Similar to {@link DomainObjectActionLinkCustomiser}, but fires the
 * object/action directly, not via history
 */
@ClientInstantiable
public class ObjectActionLinkCustomiser implements Customiser {
	public static final String ACTION_CLASS = "actionClass";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		List<PermissibleAction> actions = new ArrayList<PermissibleAction>();
		for (NamedParameter p : info.parameters()) {
			if (p.name().equals(ACTION_CLASS)) {
				Class c = p.classValue();
				actions.add((PermissibleAction) Reflections.classLookup()
						.newInstance(c));
			}
		}
		return new ObjectActionLinkProvider(actions);
	}

	public static class ObjectActionLink extends AbstractBoundWidget {
		private FlowPanel fp;

		private ClickHandler actionCl = new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				PermissibleAction a = ((Link<PermissibleAction>) sender)
						.getUserObject();
				LooseActionRegistry.get().performForTargetActionAndObject(a,
						getModel());
			}
		};

		private List<PermissibleAction> vetoableActions;

		public ObjectActionLink() {
			this.fp = new FlowPanel();
			initWidget(fp);
		}

		public ObjectActionLink(List<PermissibleAction> vetoableActions,
				Object value) {
			this();
			setModel(value);
			setVetoableActions(vetoableActions);
			setValue(value);
		}

		public Object getValue() {
			return null;
		}

		public List<PermissibleAction> getVetoableActions() {
			return vetoableActions;
		}

		public void setValue(Object value) {
			setVisible(value != null);
		}

		public void
				setVetoableActions(List<PermissibleAction> vetoableActions) {
			this.vetoableActions = vetoableActions;
			for (PermissibleAction a : vetoableActions) {
				Link<PermissibleAction> hl = new Link<PermissibleAction>(
						a.getDisplayName(), actionCl);
				hl.setWordWrap(false);
				hl.setUserObject(a);
				fp.add(hl);
				fp.add(UsefulWidgetFactory.createSpacer(2));
			}
		}
	}

	public static class ObjectActionLinkProvider
			implements BoundWidgetProvider {
		private final List<PermissibleAction> actions;

		public ObjectActionLinkProvider(List<PermissibleAction> actions) {
			this.actions = actions;
		}

		public BoundWidget get() {
			ObjectActionLink l = new ObjectActionLink();
			l.setVetoableActions(actions);
			return l;
		}
	}
}