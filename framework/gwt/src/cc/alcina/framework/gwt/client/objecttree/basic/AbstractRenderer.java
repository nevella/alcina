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

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public abstract class AbstractRenderer<T extends TreeRenderable> implements
		TreeRenderer<T> {
	private T renderable;

	private AbstractBoundWidget boundWidget;

	public AbstractBoundWidget getBoundWidget() {
		return this.boundWidget;
	}

	public void setBoundWidget(AbstractBoundWidget boundWidget) {
		this.boundWidget = boundWidget;
	}

	private RenderContext context;

	private List<TreeRenderer> childRenderers = new ArrayList<TreeRenderer>();

	public CollectionFilter collectionFilter() {
		return null;
	}

	public CollectionProvider collectionProvider() {
		return null;
	}

	public Collection<? extends TreeRenderer> childRenderers() {
		return childRenderers;
	}

	public TreeRenderer childRendererForRenderableClass(
			Class<? extends TreeRenderable> clazz) {
		if (renderable.getClass() == clazz) {
			return this;
		}
		for (TreeRenderer tr : childRenderers) {
			TreeRenderer r = tr.childRendererForRenderableClass(clazz);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	public String emptyChildText() {
		return "(Not set)";
	}

	public RenderContext getContext() {
		return context;
	}

	public T getRenderable() {
		return this.renderable;
	}

	public String hint() {
		return null;
	}

	public boolean isAlwaysExpanded() {
		return false;
	}

	public boolean isSingleLineCustomiser() {
		return false;
	}

	public String renderablePropertyName() {
		return getRenderable() != null && (getRenderable() instanceof HasValue) ? "value"
				: null;
	}

	public String renderableText() {
		return getRenderable().toString();
	}

	public boolean renderChildrenHorizontally() {
		return false;
	}

	public String renderCss() {
		return null;
	}

	public BoundWidgetProvider renderCustomiser() {
		return null;
	}

	public RenderInstruction renderInstruction() {
		return RenderInstruction.IGNORE_AND_DESCEND;
	}

	public String section() {
		return null;
	}

	public void setContext(RenderContext context) {
		this.context = context;
	}

	public void setRenderable(T renderable) {
		this.renderable = renderable;
	}
	@Override
	public String title() {
		return null;
	}
}
