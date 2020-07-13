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

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.directed.RenderContext;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

/**
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public abstract class AbstractRenderer<T extends TreeRenderable>
		implements TreeRenderer<T> {
	private T renderable;

	private AbstractBoundWidget boundWidget;

	private RenderContext context;

	private List<TreeRenderer> childRenderers = new ArrayList<TreeRenderer>();

	private TreeRenderer parentRenderer;

	@Override
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

	@Override
	public Collection<? extends TreeRenderer> childRenderers() {
		return childRenderers;
	}

	@Override
	public CollectionFilter collectionFilter() {
		return null;
	}

	@Override
	public CollectionProvider collectionProvider() {
		return null;
	}

	@Override
	public String emptyChildText() {
		return "(Not set)";
	}

	@Override
	public AbstractBoundWidget getBoundWidget() {
		return this.boundWidget;
	}

	@Override
	public RenderContext getContext() {
		return context;
	}

	@Override
	public TreeRenderer getParentRenderer() {
		return this.parentRenderer;
	}

	@Override
	public T getRenderable() {
		return this.renderable;
	}

	@Override
	public String hint() {
		return null;
	}

	@Override
	public boolean isAlwaysExpanded() {
		return false;
	}

	@Override
	public boolean isSingleLineCustomiser() {
		return false;
	}

	@Override
	public String renderablePropertyName() {
		return getRenderable() != null && (getRenderable() instanceof HasValue)
				? "value"
				: null;
	}

	@Override
	public String renderableText() {
		return getRenderable().toString();
	}

	@Override
	public boolean renderChildrenHorizontally() {
		return false;
	}

	@Override
	public String renderCss() {
		return null;
	}

	@Override
	public BoundWidgetProvider renderCustomiser() {
		return null;
	}

	@Override
	public RenderInstruction renderInstruction() {
		return RenderInstruction.IGNORE_AND_DESCEND;
	}

	@Override
	public String section() {
		return null;
	}

	@Override
	public void setBoundWidget(AbstractBoundWidget boundWidget) {
		this.boundWidget = boundWidget;
	}

	@Override
	public void setContext(RenderContext context) {
		this.context = context;
	}

	@Override
	public void setParentRenderer(TreeRenderer parentRenderer) {
		this.parentRenderer = parentRenderer;
	}

	@Override
	public void setRenderable(T renderable) {
		this.renderable = renderable;
	}

	@Override
	public String title() {
		return null;
	}
}
