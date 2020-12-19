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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingHtml;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class OneToManyCustomiser implements Customiser, BoundWidgetProvider {
	private AnnotationLocation propertyLocation;

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Args {
		Class<? extends Entity> entityClass();
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params,
			AnnotationLocation propertyLocation) {
		this.propertyLocation = propertyLocation;
		return this;
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BoundWidget get() {
		RenderingHtml html = new RenderingHtml();
		html.setRenderer(new RendererImpl(html, propertyLocation));
		html.setStyleName("");
		return html;
	}

	private static class RendererImpl
			implements Renderer<Set<? extends Entity>, String> {
		private RenderingHtml html;

		private AnnotationLocation propertyLocation;

		public RendererImpl(RenderingHtml html,
				AnnotationLocation propertyLocation) {
			this.html = html;
			this.propertyLocation = propertyLocation;
		}

		@Override
		public String render(Set<? extends Entity> o) {
			Entity source = (Entity) html.getModel();
			Args args = propertyLocation.getAnnotation(Args.class);
			EntityPlace searchPlace = (EntityPlace) RegistryHistoryMapper.get()
					.getPlaceByModelClass(args.entityClass());
			TruncatedObjectCriterion objectCriterion = Registry
					.impl(TruncatedObjectCriterion.class, source.entityClass());
			objectCriterion.withObject(source);
			searchPlace.def.addCriterionToSoleCriteriaGroup(objectCriterion);
			String template = "<a href='#%s'>%s</a>";
			String token = searchPlace.toTokenString();
			return Ax.format(template, token,
					searchPlace.provideCategoryString(o.size()));
		}
	}
}
