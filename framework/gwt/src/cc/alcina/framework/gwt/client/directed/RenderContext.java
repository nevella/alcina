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
package cc.alcina.framework.gwt.client.directed;

import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.validator.ValidationFeedback;

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

	private static final String VALIDATION_FEEDBACK_SUPPLIER = RenderContext.class
			.getName() + ".VALIDATION_FEEDBACK_SUPPLIER";

	private static final String SUPPRESS_VALIDATION_FEEDBACK_FOR = RenderContext.class
			.getName() + ".SUPPRESS_VALIDATION_FEEDBACK_FOR";

	public static final String CONTEXT_IGNORE_AUTOFOCUS = ContentViewFactory.class
			.getName() + ".CONTEXT_IGNORE_AUTOFOCUS";

	public static final String ENUM_RENDERER_PROVIDER = ContentViewFactory.class
			.getName() + ".ENUM_RENDERER_PROVIDER";

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

	public Renderer getNodeTypeRenderer(TreeRenderer node) {
		Function<TreeRenderer, Renderer> nodeToEnumRenderer = get(
				ENUM_RENDERER_PROVIDER);
		return nodeToEnumRenderer == null ? null
				: nodeToEnumRenderer.apply(node);
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

	public Widget getSuppressValidationFeedbackFor() {
		return get(SUPPRESS_VALIDATION_FEEDBACK_FOR);
	}

	public Function<String, ValidationFeedback>
			getValidationFeedbackSupplier() {
		return get(VALIDATION_FEEDBACK_SUPPLIER);
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

	public void setNodeTypeRenderer(
			Function<TreeRenderer, Renderer> nodeToEnumRenderer) {
		set(ENUM_RENDERER_PROVIDER, nodeToEnumRenderer);
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

	public void setSuppressValidationFeedbackFor(Widget widget) {
		set(SUPPRESS_VALIDATION_FEEDBACK_FOR, widget);
	}

	public void setValidationFeedbackSupplier(
			Function<String, ValidationFeedback> validationFeedbackSupplier) {
		set(VALIDATION_FEEDBACK_SUPPLIER, validationFeedbackSupplier);
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
	 * FIXME - directedlayout.1 - - given it's single threaded, push/pop of
	 * snapshots probably makes even more sense...although the new thinking is
	 * "rendercontext should be widget-tree based, not call-stack-tree based
	 * </p>
	 */
	@Override
	public RenderContext snapshot() {
		RenderContext context = new RenderContext();
		cloneToSnapshot(context);
		return context;
	}

	public RenderContextStyles styles() {
		return new RenderContextStyles(this);
	}
}
