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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar.ToolbarButton;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

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

    protected Toolbar toolbar;

    protected PermissibleAction nextPage = new PermissibleAction("Next >",
            NEXT);

    protected PermissibleAction previousPage = new PermissibleAction("< Back",
            PREVIOUS);

    protected PermissibleAction finish = new PermissibleAction("Finish",
            FINISH);

    protected PermissibleAction cancel = new PermissibleAction("Cancel",
            CANCEL);

    protected ArrayList<PermissibleAction> actions;

    private boolean allButtonsEnabled = false;

    private boolean toolbarAsNativeButtons = true;

    private Widget currentWidget;

    private String styleName = "";

    private boolean renderInScrollPanel = true;

    private int contentScrollPanelHeight = 0;

    public int getContentScrollPanelHeight() {
        return this.contentScrollPanelHeight;
    }

    public M getModel() {
        return model;
    }

    public List<WizardPage> getPages() {
        return pages;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getTitle() {
        return title;
    }

    public void gotoPage(int pageNumber) {
        if (pageNumber < pages.size()) {
            pageIndex = pageNumber;
        }
    }

    public boolean isAllButtonsEnabled() {
        return allButtonsEnabled;
    }

    public boolean isRenderInScrollPanel() {
        return this.renderInScrollPanel;
    }

    public boolean isTitleIsBreadcrumbBar() {
        return titleIsBreadcrumbBar;
    }

    public boolean isToolbarAsNativeButtons() {
        return this.toolbarAsNativeButtons;
    }

    public Widget renderPage() {
        FlowPanel fp = new FlowPanel();
        fp.addAttachHandler(
                evt -> System.out.println(fp.getElement().getInnerHTML()));
        currentWidget = fp;
        fp.setStyleName(FRAME_STYLE_NAME);
        fp.addStyleName(getStyleName());
        renderHeader(fp);
        if (usePageTabs) {
            renderTabPane(fp);
            return fp;
        }
        WizardPage page = pages.get(pageIndex);
        Widget w = page.getPageWidget();
        w.addStyleName("wizard-form");
        if (contentScrollPanelHeight != 0) {
            ScrollPanel sp = new ScrollPanel();
            sp.setHeight(contentScrollPanelHeight + "px");
            sp.add(w);
            fp.add(sp);
        } else {
            fp.add(w);
        }
        renderButtonsPane(fp);
        PermissibleAction highlighted = page.getHighlightedAction();
        highlighted = highlighted == null
                ? CommonUtils.first(toolbar.getActions())
                : highlighted;
        if (highlighted == previousPage || highlighted == cancel) {
            highlighted = null;
        }
        if (highlighted != null) {
            toolbar.getButtonForAction(highlighted).addStyleName("highlighted");
        }
        toolbar.addVetoableActionListener(this);
        if (renderInScrollPanel) {
            ScrollPanel sp = new ScrollPanel();
            sp.getElement().getStyle().setPropertyPx("maxHeight",
                    Window.getClientHeight() * 70 / 100);
            fp.getElement().getStyle().setMargin(0.8, Unit.EM);
            sp.add(fp);
            currentWidget = sp;
            return sp;
        } else {
            return fp;
        }
    }

    public void setAllButtonsEnabled(boolean allButtonsEnabled) {
        this.allButtonsEnabled = allButtonsEnabled;
    }

    public void setContentScrollPanelHeight(int contentScrollPanelHeight) {
        this.contentScrollPanelHeight = contentScrollPanelHeight;
    }

    public void setModel(M model) {
        this.model = model;
    }

    public void setPages(List<WizardPage> pages) {
        this.pages = pages;
    }

    public void setRenderInScrollPanel(boolean renderInScrollPanel) {
        this.renderInScrollPanel = renderInScrollPanel;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleIsBreadcrumbBar(boolean titleIsBreadcrumbBar) {
        this.titleIsBreadcrumbBar = titleIsBreadcrumbBar;
    }

    public void setToolbarAsNativeButtons(boolean toolbarAsNativeButtons) {
        this.toolbarAsNativeButtons = toolbarAsNativeButtons;
    }

    @Override
    public void vetoableAction(PermissibleActionEvent evt) {
        if (evt.getAction() == nextPage) {
            moveNext();
        }
        if (evt.getAction() == previousPage) {
            movePrevious();
        }
        if (evt.getAction() == cancel) {
            onCancel();
        }
        if (evt.getAction() == finish) {
            onFinished();
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
        if (canCancel()) {
            actions.add(cancel);
        }
        if (canFinishOnThisPage()) {
            actions.add(finish);
        }
        this.toolbar = new Toolbar();
        toolbar.setRemoveListenersOnDetach(false);
        toolbar.setAsButton(isToolbarAsNativeButtons());
        toolbar.addStyleName("wizard-toolbar");
        toolbar.setWidth("");
        getExtraActions();
        toolbar.setActions(actions);
        if (actions.contains(cancel)) {
            toolbar.getButtonForAction(cancel).addStyleName("cancel");
        }
        if (actions.contains(finish)) {
            toolbar.getButtonForAction(finish).addStyleName("finish");
        }
        refreshButtonActivation();
        FlowPanel holder = new FlowPanel();
        holder.add(toolbar);
        holder.setStyleName("wizard-toolbar-outer");
        fp.add(holder);
    }

    private void renderHeader(FlowPanel fp) {
        if (titleIsBreadcrumbBar) {
            fp.add(new BreadcrumbBar(getTitle()));
        }
    }

    private void renderTabPane(FlowPanel fp) {
        // TODO Auto-generated method stub
    }

    protected abstract boolean beforeMoveToPage(int newPageIndex);

    protected boolean canCancel() {
        return true;
    }

    protected abstract boolean canFinishOnThisPage();

    protected boolean canMoveBack() {
        return pageIndex != 0;
    }

    protected boolean canMoveForward() {
        return pageIndex != pages.size() - 1;
    }

    // for subclasses
    protected void getExtraActions() {
    }

    protected abstract boolean isPageValid();

    protected void moveNext() {
        if (beforeMoveToPage(pageIndex + 1)) {
            pageIndex++;
            WidgetUtils.replace(currentWidget, renderPage());
            refreshButtonActivation();
        }
    }

    protected void movePrevious() {
        if (beforeMoveToPage(pageIndex - 1)) {
            pageIndex--;
            WidgetUtils.replace(currentWidget, renderPage());
            refreshButtonActivation();
        }
    }

    protected abstract void onCancel();

    protected abstract void onFinished();

    protected void refreshButtonActivation() {
        if (allButtonsEnabled) {
            return;
        }
        for (PermissibleAction action : actions) {
            ToolbarButton tb = toolbar.getButtonForAction(action);
            if (tb != null) {
                if (action == nextPage || action == finish) {
                    tb.setEnabled(isPageValid());
                }
            }
        }
    }

    public static interface WizardPage {
        public PermissibleAction getHighlightedAction();

        public Widget getPageWidget();
    }
}
