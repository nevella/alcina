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
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManySummary;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingHtml;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class OneToManySummaryCustomiser
		implements Customiser, BoundWidgetProvider {
	@Override
	public BoundWidget get() {
		RenderingHtml html = new RenderingHtml();
		html.setRenderer(new OneToManySummaryToHtmlRenderer(html));
		html.setStyleName("");
		return html;
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		return this;
	}

	private static class OneToManySummaryToHtmlRenderer
			implements Renderer<OneToManySummary, String> {
		private RenderingHtml html;

		public OneToManySummaryToHtmlRenderer(RenderingHtml html) {
			this.html = html;
		}

		@Override
		public String render(OneToManySummary o) {
			Entity source = (Entity) html.getModel();
			Entity mostRecent = o.getLastModified();
			EntityPlace instancePlace = (EntityPlace) RegistryHistoryMapper
					.get().getPlaceByModelClass(
							Reflections.forName(o.getEntityClassName()));
			EntityPlace searchPlace = instancePlace.copy();
			TruncatedObjectCriterion objectCriterion = Registry.impl(TruncatedObjectCriterion.class,source.entityClass());
			objectCriterion.withObject(source);
			searchPlace.def.addCriterionToSoleCriteriaGroup(objectCriterion);
			if (mostRecent == null) {
				String template = "<a href='#%s'>%s</a>";
				String token = searchPlace.toTokenString();
				return Ax.format(template, token,
						instancePlace.provideCategoryString(o.getSize(), true));
			} else {
				String template = "<a href='#%s'>%s</a> - most recent: %s";
				instancePlace.withEntity(mostRecent);
				String token = searchPlace.toTokenString();
				return Ax.format(template, token,
						instancePlace.provideCategoryString(o.getSize(), true),
						mostRecent.toString());
			}
		}
	}
}
