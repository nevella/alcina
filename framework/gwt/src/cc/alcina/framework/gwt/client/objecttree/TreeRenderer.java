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

import java.util.Collection;

import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * Note - rather than having this in a separate class to the (CriteriaGroup)
 * object, these methods are kept in the class for easier overriding - even
 * though the classes can get sorta messy.
 * 
 * Otherwise access can be a problem. Remember, criteriagroups are really more
 * UI objects, it's the criterion that count more in search (if using
 * projections).
 * 
 * umm...now I'm not convinced neither. ok, all detached...
 * 
 * @author Nick Reddel
 */
public interface TreeRenderer<T extends TreeRenderable> {
	public CollectionProvider collectionProvider(RenderContext context);

	public String emptyChildText();

	public T getRenderable();

	public String hint(RenderContext context);

	public boolean isSingleLineCustomiser(RenderContext context);

	public Collection<? extends TreeRenderable> renderableChildren();

	public String renderablePropertyName();

	public boolean renderChildrenHorizontally();

	public String renderCss();

	public String renderableText();

	public BoundWidgetProvider renderCustomiser(RenderContext context);

	public RenderInstruction renderInstruction();

	public String section();

	public void setRenderable(T renderable);

	public enum RenderInstruction {
		AS_WIDGET, AS_TITLE, AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD,
		IGNORE_AND_DESCEND, NO_RENDER
	}
}
