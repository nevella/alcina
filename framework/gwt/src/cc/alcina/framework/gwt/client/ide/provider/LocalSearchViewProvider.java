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
import java.util.function.Predicate;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.search.LocalSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.provider.CollectionDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
import cc.alcina.framework.gwt.client.objecttree.ObjectTreeGridRenderer;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.InputButton;

/**
 *
 * @author Nick Reddel
 */
public class LocalSearchViewProvider implements ViewProvider {
	private boolean withoutCaption;

	private boolean withoutParameters;

	private List<String> ignoreProperties;

	// for subclasses
	protected int addTableMasks(int mask) {
		return mask;
	}

	private Widget createCaption(String actionDisplayName) {
		BreadcrumbBar bar = new BreadcrumbBar(actionDisplayName);
		bar.addStyleName("tlr-borders");
		return bar;
	}

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}

	public Widget getViewForObject(LocalSearchDefinition def,
			String displayName, String searchButtonTitle) {
		FlowPanel vp = new FlowPanel();
		vp.setStyleName("alcina-BeanPanel");
		if (!isWithoutCaption()) {
			vp.add(createCaption(displayName));
		}
		vp.add(new SearchPanel(def, searchButtonTitle));
		return vp;
	}

	@Override
	public Widget getViewForObject(Object obj) {
		LocalSearchDefinition def = (LocalSearchDefinition) obj;
		return getViewForObject(def, def.getDisplayName(), "Filter");
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

	private class SearchPanel extends FlowPanel implements ClickHandler {
		private InputButton button;

		private FlowPanel resultsHolder;

		private Widget beanView;

		private NiceWidthBoundTable table;

		private final LocalSearchDefinition def;

		public SearchPanel(LocalSearchDefinition def, String buttonTitle) {
			this.def = def;
			this.resultsHolder = new FlowPanel();
			beanView = new ObjectTreeGridRenderer().render(def);
			beanView.addStyleName("no-bottom");
			beanView.addStyleName(CommonUtils.simpleClassName(def.getClass()));
			add(beanView);
			this.button = new InputButton(buttonTitle);
			button.setStyleName("button-submit");
			button.addClickHandler(this);
			FlexTable ft = (FlexTable) ((ComplexPanel) beanView).getWidget(0);
			HorizontalPanel hp = new HorizontalPanel();
			hp.add(button);
			hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
			ft.setWidget(ft.getRowCount(), 1, hp);
			if (isWithoutParameters()) {
				beanView.setVisible(false);
			}
			add(resultsHolder);
			search();
		}

		@Override
		public void onClick(ClickEvent event) {
			search();
		}

		protected void search() {
			resultsHolder.clear();
			Predicate<String> predicate = n -> !ignoreProperties.contains(n);
			List<Field> fields = BeanFields.query()
					.forClass(def.getResultClass())
					.forMultipleWidgetContainer(true)
					.withEditableNamePredicate(predicate).listFields();
			int mask = BoundTableExt.HEADER_MASK;
			mask = mask | BoundTableExt.SORT_MASK;
			mask = addTableMasks(mask);
			CollectionDataProvider dp = new CollectionDataProvider(
					def.search());
			this.table = new NiceWidthBoundTable(mask, fields, dp);
			table.addStyleName("results-table");
			table.setVisible(false);
			resultsHolder.add(table);
			table.setVisible(true);
		}
	}
}
