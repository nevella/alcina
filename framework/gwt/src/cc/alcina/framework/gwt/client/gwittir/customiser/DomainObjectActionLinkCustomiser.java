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

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundHyperlink;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.AlcinaHistoryItem;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class DomainObjectActionLinkCustomiser implements Customiser {
	public static final String ACTION_NAME = "actionName";

	public static final String TARGET_CLASS = "targetClass";

	public static final String DISPLAY_NAME = "displayName";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String AS_HTML = "asHtml";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter param = NamedParameter.Support
				.getParameter(info.parameters(), TARGET_CLASS);
		Class targetClass = param == null ? null : param.classValue();
		param = NamedParameter.Support.getParameter(info.parameters(),
				RENDERER_CLASS);
		Class rendererClass = param == null ? null : param.classValue();
		param = NamedParameter.Support.getParameter(info.parameters(),
				DISPLAY_NAME);
		String displayName = param == null ? null : param.stringValue();
		param = NamedParameter.Support.getParameter(info.parameters(), AS_HTML);
		boolean asHtml = param == null ? false : param.booleanValue();
		return new DomainObjectActionLinkProvider(NamedParameter.Support
				.getParameter(info.parameters(), ACTION_NAME).stringValue(),
				targetClass, rendererClass, displayName, asHtml);
	}

	public static class DomainObjectActionLink extends BoundHyperlink {
		private String actionName;

		private Class targetClass;

		private String displayName;

		public DomainObjectActionLink() {
			setRenderer(DisplayNameRenderer.INSTANCE);
			addStyleName("nowrap");
		}

		public String getActionName() {
			return this.actionName;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public Class getTargetClass() {
			return targetClass;
		}

		public void setActionName(String actionName) {
			this.actionName = actionName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public void setModel(Object model) {
			super.setModel(model);
			if (model instanceof Entity) {
				Entity entity = (Entity) model;
				AlcinaHistoryItem info = AlcinaHistory.get()
						.createHistoryInfo();
				info.setActionName(actionName);
				info.setClassName(
						targetClass == null ? entity.getClass().getName()
								: targetClass.getName());
				info.setId(entity.getId());
				info.setLocalId(entity.getLocalId());
				setTargetHistoryToken(info.toTokenString());
				String renderedString = this.getRenderer() != null
						? (String) this.getRenderer().render(model)
						: model == null ? "" : model.toString();
				if (getDisplayName() != null) {
					renderedString = getDisplayName();
				}
				if (isAsHtml()) {
					this.base.setHTML(renderedString);
				} else {
					this.setText(renderedString);
				}
			}
		}

		public void setTargetClass(Class targetClass) {
			this.targetClass = targetClass;
		}

		@Override
		public void setValue(Object value) {
			return;
		}
	}

	public static class DomainObjectActionLinkProvider
			implements BoundWidgetProvider {
		private final String actionName;

		private final Class targetClass;

		private final String displayName;

		private final Class<? extends Renderer> rendererClass;

		private final boolean asHtml;

		public DomainObjectActionLinkProvider(String actionName,
				Class targetClass, Class rendererClass, String displayName,
				boolean asHtml) {
			this.actionName = actionName;
			this.targetClass = targetClass;
			this.rendererClass = rendererClass;
			this.displayName = displayName;
			this.asHtml = asHtml;
		}

		public BoundWidget get() {
			DomainObjectActionLink link = new DomainObjectActionLink();
			link.setActionName(actionName);
			link.setTargetClass(targetClass);
			link.setDisplayName(displayName);
			link.setAsHtml(asHtml);
			if (rendererClass != null) {
				link.setRenderer(Reflections.newInstance(rendererClass));
			}
			return link;
		}
	}
}