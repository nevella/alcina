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

package cc.alcina.framework.gwt.client.gwittir;

import java.util.Collection;

import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.search.HasWithNull;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.BoundWidgetTypeFactorySimpleGenerator;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.HasTreeRenderingInfo.RenderInstruction;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ObjectTreeRenderer {
	private OnetimeBoundWidget op;

	protected BoundWidgetTypeFactory factory = new BoundWidgetTypeFactorySimpleGenerator();

	public ComplexPanel render(HasTreeRenderingInfo root) {
		this.op = new OnetimeBoundWidget();
		renderToPanel(root, op, 0, true);
		op.getBinding().bind();
		op.getBinding().setLeft();
		op.setStyleName("alcina-ObjectTree");
		return op;
	}
	@SuppressWarnings("unchecked")
	protected void renderToPanel(HasTreeRenderingInfo node, ComplexPanel cp,
			int depth, boolean soleChild) {
		if (depth == 0 && node.renderCss() != null) {
			cp.setStyleName(node.renderCss());
		}
		boolean widgetsAdded = false;
		Collection<? extends HasTreeRenderingInfo> children = node
				.renderableChildren();
		// title
		AbstractBoundWidget customiserWidget = null;
		if (node.renderInstruction() != RenderInstruction.NO_RENDER) {
			customiserWidget = node.renderCustomiser() == null ? null
					: (AbstractBoundWidget) node.renderCustomiser().get();
		}
		switch (node.renderInstruction()) {
		case NO_RENDER:
			return;
		case AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD:
			if (customiserWidget == null && soleChild) {
				break;
			}
		case AS_TITLE:
		case AS_WIDGET:
			String displayName = node.getDisplayName();
			if (displayName != null) {
				Label label = TextProvider.get().getInlineLabel(
						TextProvider.get().getUiObjectText(
								node.getClass(),
								TextProvider.DISPLAY_NAME+"-"+displayName,
								CommonUtils
										.upperCaseFirstLetterOnly(displayName)
										+ ": "));
				label.setStyleName("level-"
						+ ((soleChild) ? Math.max(1, depth - 1) : depth));
				cp.add(label);
				widgetsAdded = true;
			}
		}
		if (customiserWidget != null) {
			// note - must be responsible for own detach - cleanup
			customiserWidget.setModel(node);
			if (node.renderCss() != null) {
				customiserWidget.addStyleName(node.renderCss());
			}
			if (node.hint() != null) {
				FlowPanel fp2 = new FlowPanel();
				Label label = new Label(node.hint());
				label.setStyleName("hint");
				fp2.add(customiserWidget);
				fp2.add(label);
				fp2.addStyleName("customiser");
				cp.add(fp2);
			} else {
				customiserWidget.addStyleName("customiser");
				cp.add(customiserWidget);
			}
			return;
		}
		if (node.renderInstruction() == RenderInstruction.AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD
				|| node.renderInstruction() == RenderInstruction.AS_WIDGET) {
			String propertyName = node.renderablePropertyName();
			Class type = GwittirBridge.get().getProperty(node, propertyName).getType();
			Field f = GwittirBridge.get().getField(node.getClass(),
					propertyName, true, false);
			if (f.getCellProvider() instanceof ListBoxEnumProvider
					&& node instanceof HasWithNull) {
				((ListBoxEnumProvider) f.getCellProvider())
						.setWithNull(((HasWithNull) node).isWithNull());
			}
			AbstractBoundWidget bw = (AbstractBoundWidget) f.getCellProvider()
					.get();
			op.getBinding().getChildren().add(
					new Binding(bw, "value", null, null, node, propertyName,
							null, null));
			if (node.renderCss() != null) {
				bw.setStyleName(node.renderCss());
			}
			bw.setStyleName("level-"
					+ ((soleChild) ? Math.max(1, depth - 1) : depth)
					+ "-widget");
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
				cp.add(childPanel);
			}
			for (HasTreeRenderingInfo child : node.renderableChildren()) {
				renderToPanel(child, childPanel, depth + 1, node
						.renderableChildren().size() == 1);
			}
		}
		return;
	}

	public static class OnetimeBoundWidget extends FlowPanel {
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
