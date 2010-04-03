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
package cc.alcina.framework.gwt.client.objecttree.basic;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

@RegistryLocation(j2seOnly = false, registryPoint = TreeRenderer.class, targetClass = SearchCriterion.class)
/**
 *
 * @author Nick Reddel
 */
public class SearchCriterionRenderer<T extends SearchCriterion> extends AbstractRenderer<T> {
	public Collection<? extends TreeRenderable> renderableChildren() {
		return null;
	}

	@Override
	public String renderCss() {
		return null;
	}

	@Override
	public RenderInstruction renderInstruction() {
		return RenderInstruction.AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD;
	}
}
