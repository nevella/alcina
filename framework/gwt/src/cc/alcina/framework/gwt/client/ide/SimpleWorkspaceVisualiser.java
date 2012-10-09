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
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.Workspace.WSVisualModel;
import cc.alcina.framework.gwt.client.ide.WorkspaceView.DataTreeView;
import cc.alcina.framework.gwt.client.ide.widget.StackPanel100pcHeight;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.widget.HasFirstFocusable;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class SimpleWorkspaceVisualiser extends Composite implements
		HasLayoutInfo {
	private final WSVisualModel model;

	protected SplitLayoutPanel hsp;

	private StackPanel100pcHeight viewHolder;

	public StackPanel100pcHeight getViewHolder() {
		return this.viewHolder;
	}

	protected Widget contentContainer;

	private VerticalPanel verticalPanel;

	private Toolbar toolbar;

	public static double defaultSplitterPosition = 280;

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
		this.hsp = new SplitLayoutPanel();
		this.viewHolder = new StackPanel100pcHeight();
		// viewHolder.setHeight("100%");
		viewHolder.setWidth("100%");
		viewHolder.setStyleName("workspaceViews "
				+ model.getViewAreaClassName());
		List<WorkspaceView> views = model.getViews();
		for (WorkspaceView view : views) {
			viewHolder.add(view, CommonUtils.formatJ(
					"<a href='#' onfocus='blur()'>%s</a>", view.getName()),
					true);
			view.ensureDebugId("");
			view.addVetoableActionListener(actionListener);
		}
		hsp.addWest(viewHolder, defaultSplitterPosition);
		createContentContainer(hsp);
		hsp.setHeight("100%");
		// verticalPanel.setHeight("100%");
		verticalPanel.setWidth("100%");
		this.toolbar = new Toolbar();
		toolbar.setActions(model.getToolbarActions());
		toolbar.setVisible(model.isToolbarVisible());
		verticalPanel.add(toolbar);
		verticalPanel.add(hsp);
		initWidget(verticalPanel);
		resetHsbPos();
	}

	@Override
	protected void onDetach() {
		super.onDetach();
	}

	protected void createContentContainer(SplitLayoutPanel hsp) {
		this.contentContainer = new ScrollPanel();
		contentContainer.setStyleName("alcina-WorkspaceContent");
		setContentWidget(model.getContentWidget());
		contentContainer.setHeight("100%");
		hsp.add(contentContainer);
	}

	public WSVisualModel getModel() {
		return this.model;
	}

	public VerticalPanel getVerticalPanel() {
		return this.verticalPanel;
	}

	void resetHsbPos() {
		hsp.setWidgetSize(viewHolder, defaultSplitterPosition);
	}

	public void setContentWidget(Widget w) {
		((SimplePanel) contentContainer).setWidget(w);
	}

	private class Resize100Vp extends VerticalPanel implements HasLayoutInfo {
		public void setHeight(String height) {
			super.setHeight(height);
			int h = Integer.valueOf(height.replace("px", "")).intValue();
			int hsph = h - toolbar.getOffsetHeight();
			SimpleWorkspaceVisualiser.this.hsp.setHeight(hsph + "px");
		};

		public LayoutInfo getLayoutInfo() {
			return new LayoutInfo() {
				public boolean to100percentOfAvailableHeight() {
					return true;
				}

				@Override
				public Iterator<Widget> getLayoutWidgets() {
					return Arrays.asList(
							new Widget[] { viewHolder, contentContainer })
							.iterator();
				}
			};
		}
	}

	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return Arrays.asList(new Widget[] { verticalPanel }).iterator();
			}
		};
	}

	public TreeItem selectNodeForObject(Object obj, boolean visibleViewOnly) {
		for (int i = 0; i < getViewHolder().getWidgetCount(); i++) {
			if (visibleViewOnly && i != getViewHolder().getSelectedIndex()) {
				continue;
			}
			Widget w = getViewHolder().getWidget(i);
			if (w instanceof DataTreeView) {
				DataTreeView dtv = (DataTreeView) w;
				TreeItem item = dtv.selectNodeForObject(obj);
				if (item != null) {
					getViewHolder().showStack(i);
					return item;
				}
			}
		}
		return null;
	}

	public void focusVisibleView() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
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
}
