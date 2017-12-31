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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

@RegistryLocation(registryPoint = TreeRenderer.class, targetClass = SearchDefinition.class)
/**
 *
 * @author Nick Reddel
 */
public class SearchDefinitionRenderer<SD extends SearchDefinition>
		extends AbstractRenderer<SD> {
	public static final String RENDER_ORDER_GROUPS = "RENDER_ORDER_GROUPS";

	public Collection<? extends TreeRenderable> renderableChildren() {
		if (getContext().getBoolean(RENDER_ORDER_GROUPS)) {
			List<CriteriaGroup> allCgs = new ArrayList<CriteriaGroup>();
			allCgs.addAll(getRenderable().getCriteriaGroups());
			allCgs.addAll(getRenderable().getOrderGroups());
			return allCgs;
		} else {
			return getRenderable().getCriteriaGroups();
		}
	}

	@Override
	public String renderCss() {
		return "search-def-panel";
	}
}
