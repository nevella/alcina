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

import java.util.List;

import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.actions.VetoableAction;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.SearchDataProvider;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
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
import com.totsp.gwittir.client.ui.table.BoundTable;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class SearchViewProvider implements ViewProvider {
	private RemoteActionWithParameters<SingleTableSearchDefinition> action;

	private boolean withoutCaption;

	private boolean withoutParameters;

	private List<String> ignoreProperties;

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}

	public Widget getViewForObject(Object obj) {
		return getViewForObject(obj, "Search");
	}

	public Widget getViewForObject(Object obj, String searchButtonTitle) {
		action = (RemoteActionWithParameters<SingleTableSearchDefinition>) obj;
		FlowPanel vp = new FlowPanel();
		vp.setStyleName("alcina-BeanPanel");
		if (!isWithoutCaption()) {
			vp.add(createCaption(action));
		}
		vp.add(new SearchPanel(action, searchButtonTitle));
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

	private Widget createCaption(VetoableAction action) {
		BreadcrumbBar bar = new BreadcrumbBar(action.getDisplayName());
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

		private NiceWidthBoundTable table;

		public SearchPanel(
				RemoteActionWithParameters<SingleTableSearchDefinition> action,
				String buttonTitle) {
			this.action = action;
			HTML description = new HTML("<i>" + action.getDescription()
					+ "</i><br />");
			this.resultsHolder = new FlowPanel();
			beanView = new ObjectTreeGridRenderer().render(action
					.getParameters());
			beanView.addStyleName("no-bottom");
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
			hp.add(runningLabel);
			ft.setWidget(ft.getRowCount(), 1, hp);
			if (isWithoutParameters()) {
				beanView.setVisible(false);
			}
			add(resultsHolder);
			search();
		}

		public void onClick(ClickEvent event) {
			search();
		}

		protected void search() {
			resultsHolder.clear();
			AsyncCallback completionCallback = new AsyncCallback() {
				public void onFailure(Throwable caught) {
					cleanup();
					ClientLayerLocator.get().clientBase().showError(caught);
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
							factory, false, true);
			GwittirBridge.get().setIgnoreProperties(null);
			int mask = BoundTable.HEADER_MASK;
			if (def.isOrderable()) {
				mask = mask | BoundTable.SORT_MASK;
			}
			mask = addTableMasks(mask);
			SearchDataProvider dp = new SearchDataProvider(def,
					completionCallback);
			this.table = new NiceWidthBoundTable(mask, factory, fields, dp);
			table.addStyleName("results-table");
			resultsHolder.add(table);
		}
	}
}
