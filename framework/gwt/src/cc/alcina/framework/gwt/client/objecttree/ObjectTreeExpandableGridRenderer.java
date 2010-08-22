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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.widget.ToggleLink;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class ObjectTreeExpandableGridRenderer extends ObjectTreeGridRenderer {
	public static final String MIN_CUSTOMISER_WIDTH = "MIN_CUSTOMISER_WIDTH";

	public static final String DEFAULT_SECTION_NAME = "DEFAULT_SECTION_NAME";

	@Override
	protected void renderToPanel(TreeRenderable renderable, ComplexPanel cp,
			int depth, boolean soleChild, RenderContext renderContext,
			TreeRenderer parent) {
		super.renderToPanel(renderable, cp, depth, soleChild, renderContext,
				parent);
		if (depth == 0) {
			cp.remove(ft);
			for (Integer level1Row : level1Rows.keySet()) {
				Widget level1ContentWidget = level1Rows.get(level1Row);
				TreeRenderer renderer = ObjectTreeExpandableGridRenderer.this.level1ContentRendererMap
						.get(level1ContentWidget);
				if (renderer.isAlwaysExpanded()) {
					continue;
				}
				final ExpandableWidgetWithRendererWrapper expandableWidgetWrapper = new ExpandableWidgetWithRendererWrapper(
						level1ContentWidget, renderer, renderContext);
				ft.setWidget(level1Row, 1, expandableWidgetWrapper);
				ft.setWidget(level1Row, colCountMax + 1, new ToggleLink(
						"[Change]", "[Finished]",
						new SelectionHandler<Integer>() {
							public void onSelection(
									SelectionEvent<Integer> event) {
								expandableWidgetWrapper.showExpanded(event
										.getSelectedItem() == 0);
								LayoutEvents.get()
										.deferRequiresGlobalRelayout();
							}
						}));
				cellFormatter.setVerticalAlignment(level1Row, colCountMax + 1,
						HasVerticalAlignment.ALIGN_TOP);
			}
			colCountMax += level1Rows.isEmpty() ? 0 : 1;
			// sort later
			String section = null;
			int rowShift = 0;
			String defaultSectionName = renderContext
					.getString(DEFAULT_SECTION_NAME);
			for (Integer i : level1RendererRows.keySet()) {
				TreeRenderer renderer = level1RendererRows.get(i);
				String rs = renderer.section();
				rs = rs == null ? defaultSectionName : rs;
				if (!CommonUtils.equalsWithNullEquality(rs, section)) {
					section = rs;
					int rowInsert = i + (rowShift++);
					ft.insertRow(rowInsert);
					ft.setWidget(rowInsert, 0, UsefulWidgetFactory
							.mediumTitleWidget(section));
					cellFormatter.setColSpan(rowInsert, 0, colCountMax);
				}
			}
			cp.add(ft);
		}
		return;
	}

	class ExpandableWidgetWithRendererWrapper extends Composite {
		private final Widget level1ContentWidget;

		private FlowPanel fp;

		private HalfBindableDisplayer bindableDisplayer;

		public ExpandableWidgetWithRendererWrapper(Widget level1ContentWidget,
				TreeRenderer renderer, RenderContext renderContext) {
			this.level1ContentWidget = level1ContentWidget;
			this.fp = new FlowPanel();
			this.bindableDisplayer = new HalfBindableDisplayer(renderer);
			String minWidth = renderContext
					.getString(ObjectTreeExpandableGridRenderer.MIN_CUSTOMISER_WIDTH);
			if (minWidth != null) {
				bindableDisplayer.setWidth(minWidth);
			}
			fp.add(bindableDisplayer);
			fp.add(level1ContentWidget);
			showExpanded(false);
			initWidget(fp);
		}

		private void showExpanded(boolean b) {
			level1ContentWidget.setVisible(b);
			bindableDisplayer.setVisible(!b);
			bindableDisplayer.updateText();
		}
	}

	public static class HalfBindableDisplayer extends Composite implements
			PropertyChangeListener {
		private Label label;

		private final TreeRenderer renderer;

		public HalfBindableDisplayer(TreeRenderer renderer) {
			this.renderer = renderer;
			this.label = new Label();
			initWidget(label);
		}

		@Override
		protected void onAttach() {
			updateText();
			renderer.getRenderable().addPropertyChangeListener(this);
			super.onAttach();
		}

		@Override
		protected void onDetach() {
			renderer.getRenderable().removePropertyChangeListener(this);
			super.onDetach();
		}

		private void updateText() {
			String text = renderer.renderableText();
			boolean empty = CommonUtils.isNullOrEmpty(text);
			if (empty) {
				text = renderer.emptyChildText();
				label.addStyleName("italic");
			} else {
				label.removeStyleName("italic");
			}
			label.setText(text);
		}

		public void propertyChange(PropertyChangeEvent evt) {
			updateText();
		}
	}
}
