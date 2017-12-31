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

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.objecttree.IsRenderableFilter;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

/**
 * 
 * @author Nick Reddel
 */
public class RenderContext extends LooseContextInstance {
	private static final String ON_DETACH_CALLBACK = RenderContext.class
			.getName() + ".ON_DETACH_CALLBACK";

	private static final String ON_ATTACH_CALLBACK = RenderContext.class
			.getName() + ".ON_ATTACH_CALLBACK";

	private static final String ROOT_RENDERER = RenderContext.class.getName()
			+ ".ROOT_RENDERER";

	private static final String IS_RENDERABLE_FILTER = RenderContext.class
			.getName() + ".IS_RENDERABLE_FILTER";

	public static final String CONTEXT_IGNORE_AUTOFOCUS = ContentViewFactory.class
			.getName() + ".CONTEXT_IGNORE_AUTOFOCUS";

	private static RenderContext trunk = null;

	/**
	 * In the case of object tree rendering, it makes sense to temporarily make
	 * the get() instance totally independent - until the initial (sychronous)
	 * setup has finished
	 */
	public static RenderContext branch() {
		if (trunk != null) {
			throw new RuntimeException(
					"Branching from already branched RenderContext");
		}
		trunk = get();
		Registry.registerSingleton(RenderContext.class, trunk.snapshot());
		return get();
	}

	public static RenderContext get() {
		RenderContext singleton = Registry.checkSingleton(RenderContext.class);
		if (singleton == null) {
			singleton = new RenderContext();
			Registry.registerSingleton(RenderContext.class, singleton);
		}
		return singleton;
	}

	public static void merge() {
		Registry.registerSingleton(RenderContext.class, trunk);
		trunk = null;
	}

	private RenderContext() {
		super();
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
			getOnAttachCallback().apply(widget);
		}
	}

	public void onDetach(Widget widget) {
		if (getOnDetachCallback() != null) {
			getOnDetachCallback().apply(widget);
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

	/**
	 * Snapshotting is sort of complex - basically, we want two contradictory
	 * things:
	 * <ul>
	 * <li>Ease of access - via RenderContext.get()
	 * <li>The ability to freeze sets of parameters
	 * </ul>
	 * <p>
	 * We use snapshot() for bound tables (which means RenderContext.get() won't
	 * work during setup), but branch()/merge() for object trees (heavier use of
	 * alcina) (.get() *will* work during setup)
	 * 
	 * TODO - given it's single threaded, push/pop of snapshots probably makes
	 * even more sense...
	 * </p>
	 */
	@Override
	public RenderContext snapshot() {
		RenderContext context = new RenderContext();
		cloneToSnapshot(context);
		return context;
	}
}
