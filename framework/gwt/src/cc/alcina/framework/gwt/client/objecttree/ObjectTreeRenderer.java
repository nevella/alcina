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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.search.HasWithNull;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.BoundWidgetTypeFactorySimpleGenerator;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer.RenderInstruction;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

/**
 * 
 * @author Nick Reddel
 */
public class ObjectTreeRenderer {
	public static final String SEARCH_SECTIONS = "SEARCH_SECTIONS";

	private FlowPanelWithBinding op;

	protected BoundWidgetTypeFactory factory = new BoundWidgetTypeFactorySimpleGenerator();

	protected Map<Widget, TreeRenderer> level1ContentRendererMap = new HashMap<Widget, TreeRenderer>();

	protected Map<Widget, TreeRenderer> level1LabelMap = new HashMap<Widget, TreeRenderer>();

	public ComplexPanel render(TreeRenderable root) {
		return render(root, RenderContext.get());
	}

	public ComplexPanel render(TreeRenderable root, RenderContext renderContext) {
		this.op = new FlowPanelWithBinding();
		op.setRenderContext(renderContext.snapshot());
		renderToPanel(root, op, 0, true, renderContext, null);
		op.getBinding().bind();
		op.getBinding().setLeft();
		op.setStyleName("alcina-ObjectTree");
		return op;
	}

	@SuppressWarnings("unchecked")
	protected void renderToPanel(TreeRenderable renderable, ComplexPanel cp,
			int depth, boolean soleChild, RenderContext renderContext,
			TreeRenderer parent) {
		if (renderable instanceof Permissible) {
			Permissible permissible = (Permissible) renderable;
			if (!PermissionsManager.get().isPermissible(permissible)) {
				return;
			}
		}
		TreeRenderer node = TreeRenderingInfoProvider.get().getForRenderable(
				renderable, renderContext);
		if (parent != null) {
			parent.childRenderers().add(node);
		} else {
			renderContext.setRootRenderer(node);
		}
		if (depth == 0 && node.renderCss() != null) {
			cp.setStyleName(node.renderCss());
		}
		boolean widgetsAdded = false;
		Collection<? extends TreeRenderer> children = node.renderableChildren();
		// title
		AbstractBoundWidget customiserWidget = null;
		RenderInstruction renderInstruction = node.renderInstruction();
		IsRenderableFilter renderableFilter = renderContext
				.getRenderableFilter();
		if (renderableFilter != null
				&& !renderableFilter.isRenderable(renderable, node)) {
			renderInstruction = RenderInstruction.NO_RENDER;
		}
		if (renderInstruction != RenderInstruction.NO_RENDER) {
			customiserWidget = node.renderCustomiser() == null ? null
					: (AbstractBoundWidget) node.renderCustomiser().get();
		}
		level1ContentRendererMap.put(customiserWidget, node);
		switch (renderInstruction) {
		case NO_RENDER:
			return;
		case AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD:
			if (customiserWidget == null && soleChild) {
				break;
			}
		case AS_TITLE:
		case AS_WIDGET:
			String displayName = renderable.getDisplayName();
			if (displayName != null) {
				Label label = TextProvider.get().getInlineLabel(
						TextProvider.get().getUiObjectText(
								node.getClass(),
								TextProvider.DISPLAY_NAME + "-" + displayName,
								CommonUtils
										.upperCaseFirstLetterOnly(displayName)
										+ ": "));
				label.setStyleName("level-"
						+ ((soleChild) ? Math.max(1, depth - 1) : depth));
				cp.add(label);
				if (depth == 1) {
					level1LabelMap.put(label, node);
				}
				widgetsAdded = true;
			}
		}
		if (customiserWidget != null) {
			// note - must be responsible for own detach - cleanup
			customiserWidget.setModel(renderable);
			node.setBoundWidget(customiserWidget);
			if (node.renderCss() != null) {
				customiserWidget.addStyleName(node.renderCss());
			}
			String customiserStyleName = node.isSingleLineCustomiser() ? "single-line-customiser"
					: "customiser";
			String hint = node.hint();
			if (hint != null && hint.startsWith(TreeRenderer.TOOLTIP_HINT)) {
				hint = hint.substring(TreeRenderer.TOOLTIP_HINT.length());
				customiserWidget.setTitle(hint);
				hint = null;
			}
			if (hint != null) {
				FlowPanel fp2 = new FlowPanel();
				Label label = new Label(hint);
				label.setStyleName("hint");
				fp2.add(customiserWidget);
				fp2.add(label);
				fp2.addStyleName(customiserStyleName);
				cp.add(fp2);
			} else {
				customiserWidget.addStyleName(customiserStyleName);
				cp.add(customiserWidget);
			}
			return;
		}
		if (node.renderInstruction() == RenderInstruction.AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD
				|| node.renderInstruction() == RenderInstruction.AS_WIDGET) {
			AbstractBoundWidget bw = new ObjectTreeBoundWidgetCreator()
					.createBoundWidget(renderable, depth, soleChild, node,
							op.getBinding());
			node.setBoundWidget(bw);
			cp.add(bw);
			widgetsAdded = true;
		}
		if (children != null && children.size() != 0) {
			ComplexPanel childPanel = cp;
			if (depth != 0) {
				if (node.renderChildrenHorizontally()) {
					HorizontalPanel hp = new HorizontalPanel();
					hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
					childPanel = hp;
				} else {
					childPanel = new FlowPanel();
				}
			}
			if (childPanel != cp) {
				level1ContentRendererMap.put(childPanel, node);
				cp.add(childPanel);
			}
			List<? extends TreeRenderable> childRenderables = new ArrayList<TreeRenderable>(
					node.renderableChildren());
			maybeSortChildRenderables(childRenderables, renderContext);
			for (TreeRenderable child : childRenderables) {
				renderToPanel(child, childPanel, depth + 1, node
						.renderableChildren().size() == 1, renderContext, node);
			}
		}
		return;
	}

	private void maybeSortChildRenderables(
			List<? extends TreeRenderable> childRenderables,
			final RenderContext renderContext) {
		if (renderContext.get(SEARCH_SECTIONS) != null) {
			final List<String> sectionOrder = renderContext
					.get(SEARCH_SECTIONS);
			Collections.sort(childRenderables,
					new Comparator<TreeRenderable>() {
						Map<TreeRenderable, Integer> lkp = new HashMap<TreeRenderable, Integer>();

						private int getIndex(TreeRenderable r) {
							if (!lkp.containsKey(r)) {
								TreeRenderer node1 = TreeRenderingInfoProvider
										.get().getForRenderable(r,
												renderContext);
								String s1 = node1.section();
								lkp.put(r, sectionOrder.indexOf(s1));
							}
							return lkp.get(r);
						}

						@Override
						public int compare(TreeRenderable o1, TreeRenderable o2) {
							return CommonUtils.compareInts(getIndex(o1),
									getIndex(o2));
						}
					});
		}
	}

	public static class ObjectTreeBoundWidgetCreator {
		public AbstractBoundWidget createBoundWidget(TreeRenderable renderable,
				int depth, boolean soleChild, TreeRenderer node,
				Binding parentBinding) {
			String propertyName = node.renderablePropertyName();
			Class type = GwittirBridge.get()
					.getProperty(renderable, propertyName).getType();
			Field f = GwittirBridge.get().getField(renderable.getClass(),
					propertyName, true, false);
			RelativePopupValidationFeedback vf = new RelativePopupValidationFeedback(
					RelativePopupValidationFeedback.BOTTOM,f.getFeedback());
			vf.addCssBackground();
			if (f.getCellProvider() instanceof ListBoxEnumProvider
					&& renderable instanceof HasWithNull) {
				((ListBoxEnumProvider) f.getCellProvider())
						.setWithNull(((HasWithNull) renderable).isWithNull());
			}
			if (f.getCellProvider() instanceof ListBoxCollectionProvider
					&& node.collectionFilter() != null) {
				ListBoxCollectionProvider lbcp = (ListBoxCollectionProvider) f
						.getCellProvider();
				lbcp.setFilter(node.collectionFilter());
			}
			AbstractBoundWidget bw = null;
			try {
				bw = (AbstractBoundWidget) f.getCellProvider().get();
			} catch (Exception e) {
				throw new WrappedRuntimeException(
						"Exception rendering object tree", e);
			}
			Binding binding = BindingBuilder.bind(bw).onLeftProperty("value")
					.validateLeftWith(f.getValidator())
					.notifiedWithLeft(f.getFeedback() != null ? vf : null)
					.toRight(renderable).onRightProperty(propertyName)
					.convertRightWith(f.getConverter()).toBinding();
			if (parentBinding != null) {
				parentBinding.getChildren().add(binding);
			}
			if (node.renderCss() != null) {
				bw.setStyleName(node.renderCss());
			}
			bw.addStyleName("level-"
					+ ((soleChild) ? Math.max(1, depth - 1) : depth)
					+ "-widget");
			return bw;
		}
	}

	public interface SupportsAttachDetachCallbacks {
		public void setRenderContext(RenderContext renderContext);
	}

	public static class FlowPanelWithBinding extends FlowPanel implements
			SupportsAttachDetachCallbacks {
		@Override
		protected void onDetach() {
			super.onDetach();
			if (renderContext != null) {
				renderContext.onDetach(this);
			}
			binding.unbind();
		}

		private Binding binding = new Binding();

		private RenderContext renderContext;

		public void setBinding(Binding binding) {
			this.binding = binding;
		}

		public Binding getBinding() {
			return binding;
		}

		@Override
		protected void onAttach() {
			super.onAttach();
			if (renderContext != null) {
				renderContext.onAttach(this);
			}
		}

		public void setRenderContext(RenderContext renderContext) {
			this.renderContext = renderContext;
		}
	}

	public static class HorizontalPanelWithBinding extends HorizontalPanel {
		@Override
		protected void onDetach() {
			super.onDetach();
			binding.unbind();
		}

		private Binding binding = new Binding();

		public void setBinding(Binding binding) {
			this.binding = binding;
		}

		public Binding getBinding() {
			return binding;
		}
	}
}
