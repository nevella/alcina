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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingHtml;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class ModelPlaceCustomiser implements Customiser, BoundWidgetProvider {
	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		return this;
	}

	@Override
	public BoundWidget get() {
		RenderingHtml html = new RenderingHtml();
		html.setRenderer(new ModelPlaceRenderer(html));
		html.setStyleName("");
		return html;
	}

	private static class ModelPlaceRenderer
			implements Renderer<Object, String> {
		private RenderingHtml html;

		public ModelPlaceRenderer(RenderingHtml html) {
			this.html = html;
		}

		@Override
		public String render(Object source) {
			if (source == null) {
				return "";
			}
			Object hasDisplayName = source;
			BasePlace place = null;
			Entity entity = null;
			if (source instanceof Entity) {
				entity = (Entity) source;
			} else if (html.getModel() instanceof Entity) {
				entity = (Entity) html.getModel();
			} else if (html.getModel() instanceof BasePlace) {
				place = (BasePlace) html.getModel();
			} else if (source instanceof BasePlace) {
				place = (BasePlace) source;
			}
			if (entity != null) {
				EntityPlace instancePlace = (EntityPlace) RegistryHistoryMapper
						.get().getPlaceByModelClass(entity.entityClass());
				instancePlace.withEntity(entity);
				place = instancePlace;
				hasDisplayName = entity;
			}
			String template = "<a href='#%s'>%s</a>";
			String token = place.toTokenString();
			return Ax.format(template, token, SafeHtmlUtils.htmlEscape(
					((HasDisplayName) hasDisplayName).displayName()));
		}
	}
}
