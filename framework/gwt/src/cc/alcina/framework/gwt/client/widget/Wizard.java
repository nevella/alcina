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

package cc.alcina.framework.gwt.client.widget;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar.ToolbarButton;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */

 public abstract class Wizard<M> implements PermissibleActionListener {
	private static final String NEXT = "next";

	private static final String PREVIOUS = "previous";

	private static final String FINISH = "finish";

	private static final String CANCEL = "cancel";

	public static String FRAME_STYLE_NAME = "alcina-Wizard";

	private List<WizardPage> pages;

	private boolean titleIsBreadcrumbBar = true;

	private boolean usePageTabs = false;

	private String title;

	protected int pageIndex;

	private M model;

	private Toolbar toolbar;

	private PermissibleAction nextPage = new PermissibleAction("Next >", NEXT);

	private PermissibleAction previousPage = new PermissibleAction("< Back", PREVIOUS);

	private PermissibleAction finished = new PermissibleAction("Finish", FINISH);

	private PermissibleAction cancel = new PermissibleAction("Cancel", CANCEL);

	protected ArrayList<PermissibleAction> actions;

	protected boolean canCancel() {
		return true;
	}

	// for subclasses
	protected void getExtraActions() {
	}

	protected abstract boolean canFinish();

	protected abstract boolean canFinishOnThisPage();

	protected abstract void onFinished();

	protected abstract void onCancel();

	protected abstract boolean isPageValid();

	protected abstract boolean beforeMoveToPage(int newPageIndex);

	protected boolean canMoveBack() {
		return pageIndex != 0;
	}

	protected boolean canMoveForward() {
		return pageIndex != pages.size() - 1;
	}

	public M getModel() {
		return model;
	}

	public List<WizardPage> getPages() {
		return pages;
	}

	public String getTitle() {
		return title;
	}

	public boolean isTitleIsBreadcrumbBar() {
		return titleIsBreadcrumbBar;
	}

	private boolean allButtonsEnabled = false;

	private void refreshButtonActivation() {
		toolbar.enableAll(true);
		if (allButtonsEnabled) {
			return;
		}
		for (PermissibleAction action : actions) {
			ToolbarButton tb = toolbar.getButtonForAction(action);
			if (tb != null) {
				if (action == nextPage || action == finished) {
					tb.setEnabled(isPageValid());
				}
			}
		}
	}

	private void renderButtonsPane(FlowPanel fp) {
		actions = new ArrayList<PermissibleAction>();
		if (canMoveBack()) {
			actions.add(previousPage);
		}
		if (canMoveForward()) {
			actions.add(nextPage);
		}
		if (canFinishOnThisPage()) {
			actions.add(finished);
		}
		if (canCancel()) {
			actions.add(cancel);
		}
		this.toolbar = new Toolbar();
		toolbar.setRemoveListenersOnDetach(false);
		toolbar.setAsButton(true);
		toolbar.addStyleName("wizard-toolbar");
		toolbar.setWidth("");
		getExtraActions();
		toolbar.setActions(actions);
		refreshButtonActivation();
		fp.add(toolbar);
	}

	private void renderHeader(FlowPanel fp) {
		if (titleIsBreadcrumbBar) {
			fp.add(new BreadcrumbBar(getTitle()));
		}
	}

	private Widget currentWidget;

	private String styleName = "";

	public Widget renderPage() {
		FlowPanel fp = new FlowPanel();
		currentWidget = fp;
		fp.setStyleName(FRAME_STYLE_NAME);
		fp.addStyleName(getStyleName());
		renderHeader(fp);
		if (usePageTabs) {
			renderTabPane(fp);
		} else {
			Widget w = pages.get(pageIndex).getWidget();
			w.addStyleName("wizard-form");
			fp.add(w);
			renderButtonsPane(fp);
			toolbar.addVetoableActionListener(this);
		}
		return fp;
	}

	private void renderTabPane(FlowPanel fp) {
		// TODO Auto-generated method stub
	}

	public void setModel(M model) {
		this.model = model;
	}

	public void setPages(List<WizardPage> pages) {
		this.pages = pages;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTitleIsBreadcrumbBar(boolean titleIsBreadcrumbBar) {
		this.titleIsBreadcrumbBar = titleIsBreadcrumbBar;
	}

	public void vetoableAction(PermissibleActionEvent evt) {
		if (evt.getAction() == nextPage) {
			if (beforeMoveToPage(pageIndex + 1)) {
				pageIndex++;
				WidgetUtils.replace(currentWidget, renderPage());
				refreshButtonActivation();
			}
		}
		if (evt.getAction() == previousPage) {
			if (beforeMoveToPage(pageIndex - 1)) {
				pageIndex--;
				WidgetUtils.replace(currentWidget, renderPage());
				refreshButtonActivation();
			}
		}
		if (evt.getAction() == cancel) {
			onCancel();
		}
		if (evt.getAction() == finished) {
			onFinished();
		}
	}

	public void setStyleName(String styleName) {
		this.styleName = styleName;
	}

	public String getStyleName() {
		return styleName;
	}

	public void setAllButtonsEnabled(boolean allButtonsEnabled) {
		this.allButtonsEnabled = allButtonsEnabled;
	}

	public boolean isAllButtonsEnabled() {
		return allButtonsEnabled;
	}

	public static interface WizardPage {
		public Widget getWidget();
	}
}
