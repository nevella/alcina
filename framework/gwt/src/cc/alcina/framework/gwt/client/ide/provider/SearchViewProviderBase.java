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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.LocalActionWithParameters;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.SearchDataProvider;
import cc.alcina.framework.gwt.client.gwittir.SearchDataProvider.SearchDataProviderCommon;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.objecttree.ObjectTreeGridRenderer;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar.BreadcrumbBarButton;
import cc.alcina.framework.gwt.client.widget.InputButton;

/**
 *
 * @author Nick Reddel
 */
public abstract class SearchViewProviderBase implements ViewProvider {
	public static final String CONTEXT_NO_INITIAL_SEARCH = SearchViewProviderBase.class
			.getName() + ".CONTEXT_NO_INITIAL_SEARCH";

	protected LocalActionWithParameters<SingleTableSearchDefinition> action;

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

	private String searchButtonTitle;

	protected Converter converter;

	private boolean editableToggle = false;

	private BreadcrumbBarEditableWidgetsButton editableToggleButton;

	// for subclasses
	protected int addTableMasks(int mask) {
		return mask;
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
		if (isEditableToggle()) {
			editableToggleButton = new BreadcrumbBarEditableWidgetsButton();
			maxButtonArr.add(0, editableToggleButton);
		}
		BreadcrumbBar bar = new BreadcrumbBar(null, history, maxButtonArr);
		bar.addStyleName("tlr-borders");
		return bar;
	}

	protected abstract SearchDataProvider createSearchDataProvider(
			AsyncCallback completionCallback, SingleTableSearchDefinition def);

	public LocalActionWithParameters<SingleTableSearchDefinition> getAction() {
		return this.action;
	}

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}

	public SearchPanel getSearchPanel() {
		return this.searchPanel;
	}

	protected List<PermissibleAction> getTableActions() {
		return null;
	}

	@Override
	public Widget getViewForObject(Object obj) {
		return getViewForObject(obj, "Search");
	}

	public Widget getViewForObject(Object obj, String searchButtonTitle) {
		return getViewForObject(obj, searchButtonTitle, null);
	}

	public Widget getViewForObject(Object obj, String searchButtonTitle,
			Converter converter) {
		this.searchButtonTitle = searchButtonTitle;
		this.converter = converter;
		action = (LocalActionWithParameters<SingleTableSearchDefinition>) obj;
		vp = new FlowPanel();
		vp.setStyleName("alcina-BeanPanel");
		if (!isWithoutCaption()) {
			vp.add(createCaption(action));
		}
		searchPanel = new SearchPanel();
		vp.add(searchPanel);
		return vp;
	}

	public boolean isEditableToggle() {
		return this.editableToggle;
	}

	public boolean isEditableWidgets() {
		return this.editableWidgets;
	}

	public boolean isInitialSearch() {
		return initialSearch;
	}

	public boolean isWithLinkedChanges() {
		return this.withLinkedChanges;
	}

	public boolean isWithMaximise() {
		return this.withMaximise;
	}

	public boolean isWithoutCaption() {
		return withoutCaption;
	}

	public boolean isWithoutParameters() {
		return withoutParameters;
	}

	public void setEditableToggle(boolean editableToggle) {
		this.editableToggle = editableToggle;
	}

	public void setEditableWidgets(boolean editableWidgets) {
		this.editableWidgets = editableWidgets;
	}

	public void setIgnoreProperties(List<String> ignoreProperties) {
		this.ignoreProperties = ignoreProperties;
	}

	public void setInitialSearch(boolean initialSearch) {
		this.initialSearch = initialSearch;
	}

	public void setWithLinkedChanges(boolean withLinkedChanges) {
		this.withLinkedChanges = withLinkedChanges;
	}

	public void setWithMaximise(boolean withMaximise) {
		this.withMaximise = withMaximise;
	}

	public void setWithoutCaption(boolean withoutCaption) {
		this.withoutCaption = withoutCaption;
	}

	public void setWithoutParameters(boolean withoutParameters) {
		this.withoutParameters = withoutParameters;
	}

	class BreadcrumbBarEditableWidgetsButton extends BreadcrumbBarButton
			implements ClickHandler {
		public BreadcrumbBarEditableWidgetsButton() {
			super();
			addClickHandler(this);
			updateText();
		}

		@Override
		public void onClick(ClickEvent event) {
			editableWidgets = !editableWidgets;
			searchPanel.search();
		}

		private void updateText() {
			setText(editableWidgets ? "View" : "Edit");
		}
	}

	public class SearchPanel extends FlowPanel implements ClickHandler {
		private InputButton button;

		private FlowPanel resultsHolder;

		private Label runningLabel;

		private Widget beanView;

		private BoundTableExt table;

		public SearchPanel() {
			try {
				HTML description = new HTML(
						"<i>" + action.getDescription() + "</i><br />");
				this.resultsHolder = new FlowPanel();
				RenderContext renderContext = RenderContext.branch();
				beanView = new ObjectTreeGridRenderer()
						.render(action.getParameters(), renderContext);
				beanView.addStyleName(CommonUtils
						.simpleClassName(action.getParameters().getClass()));
				add(beanView);
				this.button = new InputButton(searchButtonTitle);
				button.setStyleName("button-submit");
				button.addClickHandler(this);
				FlexTable ft = (FlexTable) ((ComplexPanel) beanView)
						.getWidget(0);
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
				if (isInitialSearch() && !LooseContext
						.getBoolean(CONTEXT_NO_INITIAL_SEARCH)) {
					search();
				}
			} finally {
				RenderContext.merge();
			}
		}

		public BoundTableExt createTableWidget(List<Field> fields, int mask,
				SearchDataProvider dp) {
			List<PermissibleAction> actions = getTableActions();
			if (actions != null && !isEditableWidgets()) {
				Toolbar tb = new Toolbar();
				tb.setAsButton(true);
				tb.setActions(actions);
				tb.setStyleName("table-toolbar alcina-ToolbarSmall clearfix");
				resultsHolder.add(tb);
			}
			return isEditableWidgets() ? new BoundTableExt(mask, fields, dp)
					: new NiceWidthBoundTable(mask, fields, dp);
		}

		public SearchViewProviderBase getSearchViewProvider() {
			return SearchViewProviderBase.this;
		}

		@Override
		public void onClick(ClickEvent event) {
			search();
		}

		public void search() {
			beanView.addStyleName("no-bottom");
			resultsHolder.clear();
			AsyncCallback completionCallback = new AsyncCallback() {
				private void cleanup() {
					runningLabel.setVisible(false);
					button.setEnabled(true);
				}

				@Override
				public void onFailure(Throwable caught) {
					cleanup();
					throw new WrappedRuntimeException(caught);
				}

				@Override
				public void onSuccess(Object result) {
					cleanup();
				}
			};
			if (beanView != null && !isWithoutParameters()) {
				beanView.setVisible(true);
			}
			runningLabel.setVisible(true);
			button.setEnabled(false);
			SingleTableSearchDefinition def = action.getParameters();
			Object bean = Reflections.at(def.getResultClass())
					.templateInstance();
			if (converter != null) {
				bean = converter.apply(bean);
			}
			List<Field> fields = null;
			try {
				PermissionsManager.get().setOverrideAsOwnedObject(true);
				fields = BeanFields.query().forBean(bean)
						.withEditable(isEditableWidgets())
						.forMultipleWidgetContainer(true)
						.withEditableNamePredicate(
								n -> !ignoreProperties.contains(n))
						.listFields();
			} finally {
				PermissionsManager.get().setOverrideAsOwnedObject(false);
			}
			int mask = BoundTableExt.HEADER_MASK;
			if (def.isOrderable()) {
				mask = mask | BoundTableExt.SORT_MASK;
			}
			mask = addTableMasks(mask);
			if (isEditableWidgets() && converter == null) {
				converter = new RegisterObjectsConverter();
			}
			SearchDataProvider dp = createSearchDataProvider(completionCallback,
					def);
			this.table = createTableWidget(fields, mask, dp);
			table.addStyleName("results-table");
			if (linkButton != null) {
				linkButton.setTable(this.table);
			}
			resultsHolder.add(table);
		}
	}

	public static class SearchViewProvider extends SearchViewProviderBase {
		@Override
		protected SearchDataProvider createSearchDataProvider(
				AsyncCallback completionCallback,
				SingleTableSearchDefinition def) {
			return new SearchDataProviderCommon(def, completionCallback,
					converter);
		}
	}
}
