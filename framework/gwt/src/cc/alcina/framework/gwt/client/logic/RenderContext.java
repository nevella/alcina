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
package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.objecttree.IsRenderableFilter;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class RenderContext extends LooseContext {
	private static final String ON_DETACH_CALLBACK = RenderContext.class
			.getName() + ".ON_DETACH_CALLBACK";

	private static final String ON_ATTACH_CALLBACK = RenderContext.class
			.getName() + ".ON_DETACH_CALLBACK";

	private static final String ROOT_RENDERER = RenderContext.class.getName()
			+ ".ROOT_RENDERER";

	private static final String IS_RENDERABLE_FILTER = RenderContext.class
			.getName() + ".IS_RENDERABLE_FILTER";

	private static RenderContext theInstance;

	public static RenderContext get() {
		if (theInstance == null) {
			theInstance = new RenderContext();
		}
		return theInstance;
	}

	public static final String CONTEXT_IGNORE_AUTOFOCUS = ContentViewFactory.class
			.getName() + ".CONTEXT_IGNORE_AUTOFOCUS";

	private RenderContext() {
		super();
	}

	public void appShutdown() {
		theInstance = null;
	}

	public Callback<Widget> getOnAttachCallback() {
		return get(ON_ATTACH_CALLBACK);
	}

	public Callback<Widget> getOnDetachCallback() {
		return get(ON_DETACH_CALLBACK);
	}

	public IsRenderableFilter getRenderableFilter() {
		return get(IS_RENDERABLE_FILTER);
	}

	public TreeRenderer getRootRenderer() {
		return get(ROOT_RENDERER);
	}

	public void onAttach(Widget widget) {
		if (getOnAttachCallback() != null) {
			getOnAttachCallback().callback(widget);
		}
	}

	public void onDetach(Widget widget) {
		if (getOnDetachCallback() != null) {
			getOnDetachCallback().callback(widget);
		}
	}

	public void setOnAttachCallback(Callback<Widget> onAttachCallback) {
		set(ON_ATTACH_CALLBACK, onAttachCallback);
	}

	public void setOnDetachCallback(Callback<Widget> onDetachCallback) {
		set(ON_DETACH_CALLBACK, onDetachCallback);
	}

	public void setRenderableFilter(IsRenderableFilter renderableFilter) {
		set(IS_RENDERABLE_FILTER, renderableFilter);
	}

	public void setRootRenderer(TreeRenderer rootRenderer) {
		set(ROOT_RENDERER, rootRenderer);
	}

	@Override
	public RenderContext snapshot() {
		RenderContext context = new RenderContext();
		cloneToSnapshot(context);
		return context;
	}
}
