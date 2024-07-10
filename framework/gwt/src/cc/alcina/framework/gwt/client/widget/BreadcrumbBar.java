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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents.LayoutEventType;

/**
 * 
 * @author Nick Reddel
 */
public class BreadcrumbBar extends Composite {
	public static boolean asHTML = false;

	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public static ArrayList<Widget> maxButton(Widget widgetToMaximise) {
		ArrayList<Widget> l = new ArrayList<Widget>();
		BreadcrumbBarMaximiseButton max = new BreadcrumbBarMaximiseButton();
		max.setWidgetToMaximise(widgetToMaximise);
		l.add(max);
		return l;
	}

	private String title;

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
				Link link = new Link(crumb.displayName, asHTML);
				link.setHref("#" + crumb.historyToken);
				para.add(new SpanPanel(link));
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
		buttons.forEach(w -> {
			SimplePanelWrapper spw = new SimplePanelWrapper(w);
			spw.setStyleName("right");
			fp.add(spw);
		});
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
		if (titleLabel != null) {
			titleLabel.setHTML(title);
		}
	}

	public static class BreadcrumbBarButton extends Composite
			implements HasClickHandlers {
		protected APanel panel;

		private boolean asHtml;

		public BreadcrumbBarButton() {
			this.panel = new APanel();
			initWidget(panel);
			this.panel.setStyleName("button");
		}

		public BreadcrumbBarButton(String text) {
			this();
			setText(text);
		}

		public BreadcrumbBarButton(String text, boolean asHtml) {
			this();
			this.asHtml = asHtml;
			setText(text);
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return panel.addClickHandler(handler);
		}

		public void setText(String text) {
			panel.clear();
			panel.add(BreadcrumbBar.asHTML || this.asHtml ? new InlineHTML(text)
					: new InlineLabel(text));
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

		public BreadcrumbBarDropdown(String text, boolean asHtml) {
			super(text, asHtml);
			this.panel.setStyleName("dropdown");
		}
	}

	public static class BreadcrumbBarMaximiseButton extends Composite
			implements ClickHandler {
		private ToggleButton toggleButton;

		private Widget widgetToMaximise;

		public BreadcrumbBarMaximiseButton() {
			Image i1 = AbstractImagePrototype.create(images.maximise())
					.createImage();
			Image i2 = AbstractImagePrototype.create(images.minimise())
					.createImage();
			this.toggleButton = new ToggleButton(i1, i2);
			toggleButton.addStyleName("maximise");
			ClientUtils.setImageDescendantTitle(i1, "Maximise");
			ClientUtils.setImageDescendantTitle(i2, "Maximise");
			ClientUtils.setTabIndexZero(toggleButton);
			toggleButton.addClickHandler(this);
			initWidget(toggleButton);
		}

		public ToggleButton getToggleButton() {
			return this.toggleButton;
		}

		public Widget getWidgetToMaximise() {
			return widgetToMaximise;
		}

		public void onClick(ClickEvent event) {
			// note - mouseOut won't fire - so we'll need to be fancy -
			// overriding gwt code, alas
			if (toggleButton.isDown()) {
				WidgetUtils.maximiseWidget(widgetToMaximise);
			} else {
				WidgetUtils.restoreFromMaximise();
				Ui1LayoutEvents.get().fireLayoutEvent(new LayoutEvent(
						LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
			}
		}

		public void setDown(boolean down) {
			boolean wasDown = toggleButton.isDown();
			if (wasDown != down) {
				toggleButton.setDown(down);
				onClick(null);
			}
		}

		public void setTitle(String title) {
			toggleButton.setTitle(title);
		}

		public void setWidgetToMaximise(Widget widgetToMaximise) {
			this.widgetToMaximise = widgetToMaximise;
		}

		public void toggle() {
			setDown(!toggleButton.isDown());
		}
	}

	public static class BreadcrumbBarMaximiseButton2 extends Composite
			implements ClickHandler {
		private ToggleButton toggleButton;

		private Widget widgetToMaximise;

		private int top;

		private int left;

		public BreadcrumbBarMaximiseButton2() {
			String title = "Maximise";
			Image i1 = AbstractImagePrototype.create(images.maximise2())
					.createImage();
			Image i2 = AbstractImagePrototype.create(images.minimise2())
					.createImage();
			this.toggleButton = new ToggleButton(i1, i2);
			toggleButton.getUpHoveringFace().setImage(AbstractImagePrototype
					.create(images.maximise2over()).createImage());
			toggleButton.getDownHoveringFace().setImage(AbstractImagePrototype
					.create(images.minimise2over()).createImage());
			toggleButton.addClickHandler(this);
			toggleButton.setTitle(title);
			ClientUtils.setImageDescendantTitle(i1, title);
			ClientUtils.setImageDescendantTitle(i2, title);
			initWidget(toggleButton);
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return this.toggleButton.addClickHandler(handler);
		}

		public ToggleButton getToggleButton() {
			return this.toggleButton;
		}

		public Widget getWidgetToMaximise() {
			return widgetToMaximise;
		}

		public boolean isDown() {
			return this.toggleButton.isDown();
		}

		public void onClick(ClickEvent event) {
			if (toggleButton.isDown()) {
				top = Window.getScrollTop();
				left = Window.getScrollLeft();
				WidgetUtils.maximiseWidget(widgetToMaximise);
			} else {
				WidgetUtils.restoreFromMaximise();
				Ui1LayoutEvents.get().fireLayoutEvent(new LayoutEvent(
						LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
				new Timer() {
					@Override
					public void run() {
						Window.scrollTo(left, top);
					}
				}.schedule(500);
			}
		}

		public void setTitle(String title) {
			toggleButton.setTitle(title);
		}

		public void setWidgetToMaximise(Widget widgetToMaximise) {
			this.widgetToMaximise = widgetToMaximise;
		}

		public void toggle() {
			toggleButton.setDown(!toggleButton.isDown());
			onClick(null);
		}
	}
}