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
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

@RegistryLocation(j2seOnly = false, registryPoint = TreeRenderer.class, targetObject = CriteriaGroup.class)
/**
 *
 * @author Nick Reddel
 */
public class CriteriaGroupRenderer<T extends CriteriaGroup> extends
		BasicRenderer<T> {
	public Collection<? extends TreeRenderable> renderableChildren() {
		return getRenderable().getCriteria();
	}

	@Override
	public boolean renderChildrenHorizontally() {
		return true;
	}

	@Override
	public String renderCss() {
		return "sub-head";
	}

	public RenderInstruction renderInstruction() {
		return RenderInstruction.AS_TITLE;
	}

	@Override
	public String renderableText() {
		return getRenderable().asString(false, false);
	}
}
