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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * @author nick@alcina.cc
 * 
 */
public class TreeRenderingInfoProvider {
	public TreeRenderer getForRenderable(TreeRenderable renderable,RenderContext context) {
		Class rendererClass = Registry.get().lookupSingle(TreeRenderer.class,
				renderable.getClass(),true);
		TreeRenderer renderer = (TreeRenderer)CommonLocator.get().classLookup().newInstance(
				rendererClass);
		renderer.setRenderable(renderable);
		renderer.setContext(context);
		return renderer;
	}

	private TreeRenderingInfoProvider() {
		super();
	}

	private static TreeRenderingInfoProvider provider;

	public static TreeRenderingInfoProvider get() {
		if (provider == null) {
			provider = new TreeRenderingInfoProvider();
		}
		return provider;
	}

	public static void registerTreeRenderingInfoProvider(
			TreeRenderingInfoProvider provider) {
		TreeRenderingInfoProvider.provider = provider;
	}
}
