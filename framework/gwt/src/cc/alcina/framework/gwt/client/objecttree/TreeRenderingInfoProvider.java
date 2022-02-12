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
package cc.alcina.framework.gwt.client.objecttree;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;

/**
 * @author nick@alcina.cc
 */
@RegistryLocation(registryPoint = TreeRenderingInfoProvider.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
@Registration.Singleton
public class TreeRenderingInfoProvider {
	public static TreeRenderingInfoProvider get() {
		return Registry.impl(TreeRenderingInfoProvider.class);
	}

	public TreeRenderer getForRenderable(TreeRenderable renderable,
			TreeRenderer parent, RenderContext context) {
		Class<? extends TreeRenderer> rendererClass = getClassForRenderable(
				renderable, parent, context);
		TreeRenderer renderer = Reflections.newInstance(rendererClass);
		renderer.setRenderable(renderable);
		renderer.setContext(context);
		return renderer;
	}

	protected Class<? extends TreeRenderer> getClassForRenderable(
			TreeRenderable renderable, TreeRenderer parent,
			RenderContext context) {
		return Registry.query(TreeRenderer.class).addKeys(renderable.getClass())
				.registration();
	}
}
