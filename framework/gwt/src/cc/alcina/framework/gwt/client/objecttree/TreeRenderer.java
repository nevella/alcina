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

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;

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
	public static final String TOOLTIP_HINT = "(Tooltip)";

	public TreeRenderer childRendererForRenderableClass(
			Class<? extends TreeRenderable> clazz);

	public Collection<? extends TreeRenderer> childRenderers();

	public CollectionFilter collectionFilter();

	public CollectionProvider collectionProvider();

	public String emptyChildText();

	public AbstractBoundWidget getBoundWidget();

	public RenderContext getContext();

	public T getRenderable();

	public String hint();

	public boolean isAlwaysExpanded();

	public default boolean isNoTitle() {
		return false;
	}

	public boolean isSingleLineCustomiser();

	public Collection<? extends TreeRenderable> renderableChildren();

	public String renderablePropertyName();

	public String renderableText();

	public boolean renderChildrenHorizontally();

	public String renderCss();

	public BoundWidgetProvider renderCustomiser();

	public default Renderer<?, String> renderer() {
		return null;
	}

	public RenderInstruction renderInstruction();

	public String section();

	public void setBoundWidget(AbstractBoundWidget widget);

	public void setContext(RenderContext context);

	public void setParentRenderer(TreeRenderer parent);

	public void setRenderable(T renderable);

	public String title();

	TreeRenderer getParentRenderer();

	default void parentBinding(Binding binding) {
	}

	public enum RenderInstruction {
		AS_WIDGET, AS_TITLE, AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD,
		IGNORE_AND_DESCEND, NO_RENDER
	}
}
