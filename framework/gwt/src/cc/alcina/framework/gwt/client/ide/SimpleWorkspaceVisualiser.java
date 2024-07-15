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
package cc.alcina.framework.gwt.client.ide;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.Workspace.WSVisualModel;
import cc.alcina.framework.gwt.client.ide.WorkspaceView.DataTreeView;
import cc.alcina.framework.gwt.client.ide.widget.StackPanel100pcHeight;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.widget.HasFirstFocusable;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;

/**
 * 
 * @author Nick Reddel
 */
public class SimpleWorkspaceVisualiser extends Composite
		implements HasLayoutInfo, WorkspaceVisualiser {
	public static double defaultSplitterPosition = 280;

	public static int defaultSplitterSize = 8;

	private final WSVisualModel model;

	protected SplitLayoutPanel hsp;

	private StackPanel100pcHeight viewHolder;

	protected Widget contentContainer;

	private VerticalPanel verticalPanel;

	private Toolbar toolbar;

	/**
	 * Uses horizontal panels because they're tables - i.e. 100% height works
	 * ahh...correction - layout manager. but nice idea
	 * 
	 * @param model
	 */
	public SimpleWorkspaceVisualiser(WSVisualModel model,
			PermissibleActionListener actionListener) {
		this.model = model;
		this.verticalPanel = new Resize100Vp();
		this.hsp = new SplitLayoutPanel(getSplitterSize());
		this.viewHolder = new StackPanel100pcHeight();
		// viewHolder.setHeight("100%");
		viewHolder.setWidth("100%");
		viewHolder
				.setStyleName("workspaceViews " + model.getViewAreaClassName());
		List<WorkspaceView> views = model.getViews();
		for (WorkspaceView view : views) {
			viewHolder.add(view,
					Ax.format("<a href='#' onfocus='blur()'>%s</a>",
							view.getName()),
					true);
			view.getElement().setId(Document.get().createUniqueId());
			view.addVetoableActionListener(actionListener);
		}
		hsp.addWest(viewHolder, defaultSplitterPosition);
		createContentContainer(hsp);
		hsp.setHeight("100%");
		// verticalPanel.setHeight("100%");
		verticalPanel.setWidth("100%");
		this.toolbar = new Toolbar();
		toolbar.setActions(model.getToolbarActions());
		toolbar.enableAll(false);
		toolbar.setVisible(model.isToolbarVisible());
		verticalPanel.add(toolbar);
		verticalPanel.add(hsp);
		initWidget(verticalPanel);
		resetHsbPos();
		hsp.getElement().getStyle().setProperty("width", "100vw");
	}

	protected void createContentContainer(SplitLayoutPanel hsp) {
		this.contentContainer = new ScrollPanel();
		contentContainer.setStyleName("alcina-WorkspaceContent");
		setContentWidget(model.getContentWidget());
		contentContainer.setHeight("100%");
		hsp.add(contentContainer);
	}

	@Override
	public void focusVisibleView() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				int selectedIndex = viewHolder.getSelectedIndex();
				if (selectedIndex != -1) {
					Widget w = viewHolder.getWidget(selectedIndex);
					if (w instanceof HasFirstFocusable) {
						HasFirstFocusable hff = (HasFirstFocusable) w;
						hff.firstFocusable().setFocus(true);
					}
				}
			}
		});
	}

	@Override
	public Widget getContentWidget() {
		return ((SimplePanel) contentContainer).getWidget();
	}

	@Override
	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public void afterLayout() {
				resetHsbPos();
			}

			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return Arrays.asList(new Widget[] { verticalPanel }).iterator();
			}

			@Override
			public Iterator<Widget> getWidgetsToResize() {
				return (Iterator) Collections.singleton(getVerticalPanel())
						.iterator();
			}
		};
	}

	public WSVisualModel getModel() {
		return this.model;
	}

	protected int getSplitterSize() {
		return defaultSplitterSize;
	}

	public VerticalPanel getVerticalPanel() {
		return this.verticalPanel;
	}

	public StackPanel100pcHeight getViewHolder() {
		return this.viewHolder;
	}

	@Override
	protected void onDetach() {
		super.onDetach();
	}

	@Override
	public void redraw() {
		resetHsbPos();
	}

	void resetHsbPos() {
		hsp.setWidgetSize(viewHolder, defaultSplitterPosition);
	}

	@Override
	public TreeItem selectNodeForObject(Object obj, boolean visibleViewOnly) {
		for (int i = 0; i < getViewHolder().getWidgetCount(); i++) {
			if (visibleViewOnly && i != getViewHolder().getSelectedIndex()) {
				continue;
			}
			Widget w = getViewHolder().getWidget(i);
			if (w instanceof DataTreeView) {
				DataTreeView dtv = (DataTreeView) w;
				if (CommonUtils.isNotNullOrEmpty(
						dtv.getFilter().getTextBox().getText())) {
					dtv.getFilter().clear();
					dtv.getDataTree().filter("");
				}
				TreeItem item = dtv.selectNodeForObject(obj);
				if (item != null) {
					getViewHolder().showStack(i);
					return item;
				}
			}
		}
		return null;
	}

	@Override
	public void setContentWidget(Widget w) {
		((SimplePanel) contentContainer).setWidget(w);
	}

	@Override
	public void showView(WorkspaceView view) {
		viewHolder.showStack(viewHolder.getWidgetIndex(view));
	}

	private class Resize100Vp extends VerticalPanel implements HasLayoutInfo {
		@Override
		public LayoutInfo getLayoutInfo() {
			return new LayoutInfo() {
				@Override
				public Iterator<Widget> getLayoutWidgets() {
					return Arrays.asList(
							new Widget[] { viewHolder, contentContainer })
							.iterator();
				}

				@Override
				public boolean to100percentOfAvailableHeight() {
					return true;
				}
			};
		};

		@Override
		public void setHeight(String height) {
			super.setHeight(height);
			int h = Integer.valueOf(height.replace("px", "")).intValue();
			int hsph = h - toolbar.getOffsetHeight();
			SimpleWorkspaceVisualiser.this.hsp.setHeight(hsph + "px");
		}
	}
}
