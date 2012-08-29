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
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.SearchDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.objecttree.ObjectTreeGridRenderer;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.InputButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class SearchViewProvider implements ViewProvider {
	private RemoteActionWithParameters<SingleTableSearchDefinition> action;

	private boolean withoutCaption;

	private boolean withoutParameters;

	private List<String> ignoreProperties;

	private SearchPanel searchPanel;

	private boolean initialSearch = true;

	private boolean editableWidgets;

	private boolean withMaximise;

	private boolean withLinkedChanges;

	private FlowPanel vp;

	private BreadcrumbBarLinkChangesButton linkButton;

	public SearchPanel getSearchPanel() {
		return this.searchPanel;
	}

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}

	public Widget getViewForObject(Object obj) {
		return getViewForObject(obj, "Search");
	}

	public Widget getViewForObject(Object obj, String searchButtonTitle) {
		return getViewForObject(obj, searchButtonTitle, null);
	}

	public Widget getViewForObject(Object obj, String searchButtonTitle,
			Converter converter) {
		action = (RemoteActionWithParameters<SingleTableSearchDefinition>) obj;
		vp = new FlowPanel();
		vp.setStyleName("alcina-BeanPanel");
		if (!isWithoutCaption()) {
			vp.add(createCaption(action));
		}
		searchPanel = new SearchPanel(action, searchButtonTitle, converter);
		vp.add(searchPanel);
		return vp;
	}

	public boolean isWithoutCaption() {
		return withoutCaption;
	}

	public boolean isWithoutParameters() {
		return withoutParameters;
	}

	public void setIgnoreProperties(List<String> ignoreProperties) {
		this.ignoreProperties = ignoreProperties;
	}

	public void setWithoutCaption(boolean withoutCaption) {
		this.withoutCaption = withoutCaption;
	}

	public void setWithoutParameters(boolean withoutParameters) {
		this.withoutParameters = withoutParameters;
	}

	private Widget createCaption(PermissibleAction action) {
		List<SimpleHistoryEventInfo> history = Arrays
				.asList(new SimpleHistoryEventInfo[] {
						new SimpleHistoryEventInfo("Action"),
						new SimpleHistoryEventInfo(action.getDisplayName()) });
		ArrayList<Widget> maxButtonArr = BreadcrumbBar.maxButton(vp);
		if (isWithLinkedChanges()) {
			linkButton = new BreadcrumbBarLinkChangesButton();
			maxButtonArr.add(0, linkButton);
		}
		BreadcrumbBar bar = new BreadcrumbBar(null, history, maxButtonArr);
		bar.addStyleName("tlr-borders");
		return bar;
	}

	// for subclasses
	protected int addTableMasks(int mask) {
		return mask;
	}

	public class SearchPanel extends FlowPanel implements ClickHandler {
		private InputButton button;

		private final RemoteActionWithParameters<SingleTableSearchDefinition> action;

		private FlowPanel resultsHolder;

		private Label runningLabel;

		private Widget beanView;

		private BoundTableExt table;

		private Converter converter;

		public SearchPanel(
				RemoteActionWithParameters<SingleTableSearchDefinition> action,
				String buttonTitle, Converter converter) {
			this.action = action;
			this.converter = converter;
			HTML description = new HTML("<i>" + action.getDescription()
					+ "</i><br />");
			this.resultsHolder = new FlowPanel();
			RenderContext renderContext = getRenderContextAndPush();
			beanView = new ObjectTreeGridRenderer().render(
					action.getParameters(), renderContext);
			beanView.addStyleName(CommonUtils.simpleClassName(action
					.getParameters().getClass()));
			add(beanView);
			this.button = new InputButton(buttonTitle);
			button.setStyleName("button-submit");
			button.addClickHandler(this);
			FlexTable ft = (FlexTable) ((ComplexPanel) beanView).getWidget(0);
			HorizontalPanel hp = new HorizontalPanel();
			hp.add(button);
			hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
			this.runningLabel = new Label("...searching");
			this.runningLabel.setVisible(false);
			hp.add(runningLabel);
			ft.setWidget(ft.getRowCount(), 1, hp);
			if (isWithoutParameters()) {
				beanView.setVisible(false);
			}
			add(resultsHolder);
			if (isInitialSearch()) {
				search();
			}
			renderContext.pop();
		}

		public void onClick(ClickEvent event) {
			search();
		}

		public void search() {
			beanView.addStyleName("no-bottom");
			resultsHolder.clear();
			AsyncCallback completionCallback = new AsyncCallback() {
				public void onFailure(Throwable caught) {
					cleanup();
					throw new WrappedRuntimeException(caught);
				}

				public void onSuccess(Object result) {
					cleanup();
				}

				private void cleanup() {
					runningLabel.setVisible(false);
					button.setEnabled(true);
				}
			};
			if (beanView != null && !isWithoutParameters()) {
				beanView.setVisible(true);
			}
			runningLabel.setVisible(true);
			button.setEnabled(false);
			SingleTableSearchDefinition def = action.getParameters();
			Object bean = ClientReflector.get().getTemplateInstance(
					def.getResultClass());
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			GwittirBridge.get().setIgnoreProperties(ignoreProperties);
			Field[] fields = GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactory(bean,
							factory, isEditableWidgets(), true);
			GwittirBridge.get().setIgnoreProperties(null);
			int mask = BoundTableExt.HEADER_MASK;
			if (def.isOrderable()) {
				mask = mask | BoundTableExt.SORT_MASK;
			}
			mask = addTableMasks(mask);
			if (isEditableWidgets() && converter == null) {
				converter = new RegisterObjectsConverter();
			}
			SearchDataProvider dp = new SearchDataProvider(def,
					completionCallback, converter);
			this.table = isEditableWidgets() ? new BoundTableExt(mask, factory,
					fields, dp) : new NiceWidthBoundTable(mask, factory,
					fields, dp);
			table.addStyleName("results-table");
			if (linkButton != null) {
				linkButton.setTable(this.table);
			}
			resultsHolder.add(table);
		}
	}

	protected RenderContext getRenderContextAndPush() {
		RenderContext renderContext = RenderContext.get();
		renderContext.push();
		return renderContext;
	}

	public void setInitialSearch(boolean initialSearch) {
		this.initialSearch = initialSearch;
	}

	public boolean isInitialSearch() {
		return initialSearch;
	}

	public boolean isEditableWidgets() {
		return this.editableWidgets;
	}

	public void setEditableWidgets(boolean editableWidgets) {
		this.editableWidgets = editableWidgets;
	}

	public boolean isWithMaximise() {
		return this.withMaximise;
	}

	public void setWithMaximise(boolean withMaximise) {
		this.withMaximise = withMaximise;
	}

	public boolean isWithLinkedChanges() {
		return this.withLinkedChanges;
	}

	public void setWithLinkedChanges(boolean withLinkedChanges) {
		this.withLinkedChanges = withLinkedChanges;
	}
}
