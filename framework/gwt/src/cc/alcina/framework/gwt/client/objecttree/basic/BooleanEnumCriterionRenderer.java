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

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(j2seOnly = false, registryPoint = TreeRenderer.class, targetClass = BooleanEnumCriterion.class)
public class BooleanEnumCriterionRenderer extends
		EnumCriterionRenderer<BooleanEnumCriterion> {
	@Override
	public String renderablePropertyName() {
		return BooleanEnumCriterion.RENDERABLE_PROPERTY_NAME;
	}
}
