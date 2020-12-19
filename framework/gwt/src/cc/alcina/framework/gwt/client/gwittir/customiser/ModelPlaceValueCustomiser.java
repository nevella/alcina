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
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingHtml;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class ModelPlaceValueCustomiser
		implements Customiser, BoundWidgetProvider {
	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		return this;
	}

	@Override
	public BoundWidget get() {
		RenderingHtml html = new RenderingHtml();
		html.setRenderer(new ModelPlaceValueRenderer());
		html.setStyleName("");
		return html;
	}

	private static class ModelPlaceValueRenderer
			implements Renderer<Entity, String> {
		public ModelPlaceValueRenderer() {
		}

		@Override
		public String render(Entity entity) {
			if (entity == null) {
				return "(Undefined)";
			}
			String name = null;
			BasePlace place = null;
			EntityPlace instancePlace = (EntityPlace) RegistryHistoryMapper
					.get().getPlaceByModelClass(entity.entityClass());
			if (instancePlace == null) {
				return DisplayNameRenderer.INSTANCE.render(entity);
			} else {
				instancePlace.withEntity(entity);
				place = instancePlace;
				name = place.toNameString();
			}
			String template = "<a href='#%s'>%s</a>";
			String token = place.toTokenString();
			name = Ax.blankTo(name, "(Blank)");
			return Ax.format(template, token,
					SafeHtmlUtils.htmlEscape(name));
		}
	}
}
