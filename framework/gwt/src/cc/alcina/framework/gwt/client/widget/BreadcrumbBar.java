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

import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */

 public class BreadcrumbBar extends Composite {
	private String title;

	public static boolean asHTML = false;

	private final List<Widget> buttons;

	private FlowPanel fp;

	private Para titleLabel;

	public BreadcrumbBar(String title) {
		this(title, new ArrayList<SimpleHistoryEventInfo>(),
				new ArrayList<Widget>());
	}

	public BreadcrumbBar(String title, List<SimpleHistoryEventInfo> crumbs,
			List<Widget> buttons) {
		this.title = title;
		this.buttons = buttons;
		this.fp = new FlowPanel();
		fp.setStyleName("alcina-BreadcrumbBar");
		if (crumbs == null || crumbs.isEmpty()) {
			this.titleLabel = new Para(title);
			titleLabel.setStyleName("breadcrumb");
			fp.add(titleLabel);
		} else {
			int c = 0;
			for (SimpleHistoryEventInfo crumb : crumbs) {
				String className = (++c == crumbs.size()) ? "breadcrumb"
						: "breadcrumb-next";
				ParaPanel para = new ParaPanel();
				para.setStyleName(className);
				Hyperlink hl = new Hyperlink(crumb.displayName, asHTML,
						crumb.historyToken);
				hl.setStyleName("");
				para.add(hl);
				fp.add(para);
			}
		}
		addButtons();
		initWidget(fp);
	}

	private void addButtons() {
		if (buttons == null) {
			return;
		}
		for (int i = buttons.size() - 1; i >= 0; i--) {
			Widget w = buttons.get(i);
			SimplePanelWrapper spw = new SimplePanelWrapper(w);
			spw.setStyleName("right");
			fp.add(spw);
		}
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
		if (titleLabel != null) {
			titleLabel.setText(title);
		}
	}

	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public static List<Widget> maxButton(Widget widgetToMaximise) {
		ArrayList<Widget> l = new ArrayList<Widget>();
		BreadcrumbBarMaximiseButton max = new BreadcrumbBarMaximiseButton();
		max.setWidgetToMaximise(widgetToMaximise);
		l.add(max);
		return l;
	}

	public static class BreadcrumbBarMaximiseButton extends Composite implements
			ClickHandler {
		private ToggleButton toggleButton;

		private Widget widgetToMaximise;

		public BreadcrumbBarMaximiseButton() {
			this.toggleButton = new ToggleButton(AbstractImagePrototype.create(
					images.maximise()).createImage(), AbstractImagePrototype
					.create(images.minimise()).createImage());
			toggleButton.addStyleName("maximise");
			toggleButton.addClickHandler(this);
			initWidget(toggleButton);
		}

		public void setWidgetToMaximise(Widget widgetToMaximise) {
			this.widgetToMaximise = widgetToMaximise;
		}

		public Widget getWidgetToMaximise() {
			return widgetToMaximise;
		}

		public void toggle() {
			toggleButton.setDown(!toggleButton.isDown());
			onClick(null);
		}

		public void setTitle(String title) {
			toggleButton.setTitle(title);
		}

		public void onClick(ClickEvent event) {
			// note - mouseOut won't fire - so we'll need to be fancy -
			// overriding gwt code, alas
			toggleButton.setHovering(false);
			if (toggleButton.isDown()) {
				WidgetUtils.maximiseWidget(widgetToMaximise);
			} else {
				WidgetUtils.restoreFromMaximise();
				LayoutEvents.get().fireLayoutEvent(
						new LayoutEvent(
								LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
			}
		}
	}

	public static class BreadcrumbBarButton extends Composite implements
			HasClickHandlers {
		protected APanel panel;

		public BreadcrumbBarButton() {
			this.panel = new APanel();
			initWidget(panel);
			this.panel.setStyleName("button");
		}

		public BreadcrumbBarButton(String text) {
			this();
			setText(text);
		}

		public void setText(String text) {
			panel.clear();
			panel.add(BreadcrumbBar.asHTML ? new InlineHTML(text)
					: new InlineLabel(text));
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return panel.addClickHandler(handler);
		}
	}

	public static class BreadcrumbBarDropdown extends BreadcrumbBarButton {
		public BreadcrumbBarDropdown() {
			this.panel.setStyleName("dropdown");
		}

		public BreadcrumbBarDropdown(String text) {
			this();
			setText(text);
		}
	}
}