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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */

 public class SelectWithSearch<G extends Comparable, T extends Comparable>
		implements VisualFilterable, FocusHandler, HasLayoutInfo, BlurHandler {
	private static final int DELAY_TO_CHECK_FOR_CLOSING = 400;

	private FlowPanel holder;

	private FlowPanel fp;

	private Map<G, List<T>> itemMap;

	private List<G> keys;

	private ScrollPanel scroller;

	private boolean sortGroups = true;

	private boolean flowLayout;

	private boolean sortGroupContents = true;

	private String holderHeight = "92%";

	private boolean popdown = false;

	private boolean itemsHaveLinefeeds = false;

	private String hint = null;

	private List<Label> groupCaptions;

	private FocusPanel focusPanel;

	private FilterWidget filter;

	boolean waitingToFocus = false;

	private PositionablePopDown flowPanelForFocusPanel;

	private boolean closingOnClick = false;

	private Set selectedItems = new HashSet();

	private long lastClosingClickMillis;// ie doesn't do these linearly -

	private int charWidth;

	private ClickHandler clickHandler;

	private ClickHandler popdownHider;

	private ClickHandler enterHandler;// listens for enter on the filter box

	// for non-filtered items
	private LazyDataProvider<G, T> lazyProvider;

	private int topAdjust = 0;

	private String inPanelHint = null;

	private String lastFilterText = "";

	private Label hintLabel;

	private boolean focusOnAttach = false;

	private String separatorText = " ";

	private Widget[] absoluteContainers;

	private ShowHintStrategy showHintStrategy;

	// additional problem with ff
	public SelectWithSearch() {
	}

	public Widget createWidget(Map<G, List<T>> itemMap,
			ClickHandler clickHandler, int charWidth) {
		return createWidget(itemMap, clickHandler, charWidth, null);
	}

	public Widget createWidget(Map<G, List<T>> itemMap,
			ClickHandler clickHandler, int charWidth,
			LazyDataProvider<G, T> lazyProvider) {
		this.clickHandler = clickHandler;
		this.charWidth = charWidth;
		this.lazyProvider = lazyProvider;
		this.holder = isFlowLayout() ? new FlowPanel()
				: new FlowPanel100pcHeight();
		filter = new FilterWidget(hint);
		filter.getTextBox().addKeyUpHandler(selectableNavigation);
		filter.getTextBox().addKeyDownHandler(selectableNavigation);
		filter.setFocusOnAttach(isFocusOnAttach());
		filter.registerFilterable(this);
		selectableNavigation.setWrappedEnterListener(new ClickHandler() {
			// the listeners aren't registered on every source...pretty sure
			// this is logical...
			public void onClick(ClickEvent event) {
				HasClickHandlers sender = (HasClickHandlers) event.getSource();
				if (enterHandler != null) {
					WidgetUtils.fireClickOnHandler(sender, enterHandler);
				}
				if (popdown) {
					WidgetUtils.fireClickOnHandler(sender, popdownHider);
				}
			}
		});
		fp = new FlowPanel();
		fp.setStyleName("select-item-container");
		if (inPanelHint != null) {
			hintLabel = new Label(inPanelHint);
			hintLabel.setStyleName("hint");
			if (showHintStrategy != null) {
				showHintStrategy.registerHintWidget(hintLabel);
				showHintStrategy.registerFilter(filter);
			}
			fp.add(hintLabel);
		}
		groupCaptions = new ArrayList<Label>();
		popdownHider = new ClickHandler() {
			public void onClick(ClickEvent event) {
				closingOnClick = true;
				log("closing on click", null);
				flowPanelForFocusPanel.setVisible(false);
				lastClosingClickMillis = System.currentTimeMillis();
				log("closing on click finished", null);
				closingOnClick = false;
			}
		};
		if (itemMap != null) {
			setItemMap(itemMap);
		}
		this.scroller = isFlowLayout() ? new ScrollPanel(fp)
				: new ScrollPanel100pcHeight(fp);
		if (!isFlowLayout()) {
			scroller.setSize("100%", "100%");
			holder.setHeight(holderHeight);
		}
		holder.setStyleName("alcina-Chooser");
		holder.add(filter);
		if (popdown) {
			this.focusPanel = new FocusPanel();
			flowPanelForFocusPanel = new PositionablePopDown();
			flowPanelForFocusPanel.setVisible(false);
			flowPanelForFocusPanel.addStyleName("pop-down");
			flowPanelForFocusPanel.add(scroller);
			focusPanel.add(flowPanelForFocusPanel);
			focusPanel.addFocusHandler(this);
			focusPanel.addBlurHandler(this);
			filter.getTextBox().addFocusHandler(this);
			filter.getTextBox().addBlurHandler(this);
			filter.getTextBox().addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					checkShowPopup();
				}
			});
			filter.getTextBox().addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					if (Event.getCurrentEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
						if (popdown) {
							popdownHider.onClick(null);
						}
					} else {
						checkShowPopup();
					}
				}
			});
			holder.add(focusPanel);
		} else {
			holder.add(scroller);
		}
		return holder;
	}

	SelectableNavigation selectableNavigation = new SelectableNavigation();

	class SelectableNavigation implements KeyUpHandler, KeyDownHandler {
		private int selectedIndex = -1;

		private Widget lastSelected = null;

		private ClickHandler wrappedEnterListener;

		public ClickHandler getWrappedEnterListener() {
			return this.wrappedEnterListener;
		}

		public void setWrappedEnterListener(ClickHandler enterListener) {
			this.wrappedEnterListener = enterListener;
		}

		public void clear() {
			selectedIndex = -1;
			updateSelection();
		}

		private void updateSelection() {
			if (lastSelected != null) {
				lastSelected.removeStyleName("selected");
			}
			lastSelected = null;
			if (selectedIndex < -1) {
				selectedIndex = -1;
			}
			if (selectedIndex != -1) {
				Widget selectedWidget = getSelectedWidget();
				if (selectedWidget != null) {
					selectedWidget.addStyleName("selected");
					DOM.scrollIntoView(selectedWidget.getElement());
					lastSelected = selectedWidget;
				} else {
					int vfc = getVisibleFilterableCount();
					if (selectedIndex > vfc) {
						selectedIndex = vfc;
						updateSelection();
					}
				}
			}
		}

		public void onKeyDown(KeyDownEvent event) {
			int keyCode = event.getNativeKeyCode();
			if (keyCode == KeyCodes.KEY_UP || keyCode == KeyCodes.KEY_DOWN) {
				WidgetUtils.squelchCurrentEvent();
			}
		}

		public void onKeyUp(KeyUpEvent event) {
			Widget sender = (Widget) event.getSource();
			int keyCode = event.getNativeKeyCode();
			if (keyCode == KeyCodes.KEY_UP || keyCode == KeyCodes.KEY_DOWN) {
				WidgetUtils.squelchCurrentEvent();
			}
			if (keyCode == KeyCodes.KEY_UP) {
				if (selectedIndex > 0) {
					selectedIndex--;
				}
				updateSelection();
			}
			if (keyCode == KeyCodes.KEY_DOWN) {
				selectedIndex++;
				updateSelection();
			}
			boolean hidePopdown = false;
			if (keyCode == KeyCodes.KEY_ENTER) {
				if (selectedIndex != -1) {
					DomEvent.fireNativeEvent(WidgetUtils.createZeroClick(),
							getSelectedWidget());
					hidePopdown = true;
					selectedIndex = -1;
				} else {
					if (wrappedEnterListener != null) {
						WidgetUtils.fireClickOnHandler((HasClickHandlers) event
								.getSource(), wrappedEnterListener);
						hidePopdown = true;
					}
				}
			}
			if (hidePopdown && popdown) {
				popdownHider.onClick(null);
			}
		}

		private int getVisibleFilterableCount() {
			int visibleIndex = -1;
			for (int i = 0; i < fp.getWidgetCount(); i++) {
				Widget widget = fp.getWidget(i);
				if (widget instanceof VisualFilterable && widget.isVisible()) {
					visibleIndex++;
				}
			}
			return visibleIndex;
		}

		private Widget getSelectedWidget() {
			int visibleIndex = -1;
			for (int i = 0; i < fp.getWidgetCount(); i++) {
				Widget widget = fp.getWidget(i);
				if (widget instanceof VisualFilterable && widget.isVisible()) {
					visibleIndex++;
					if (selectedIndex == visibleIndex) {
						return widget;
					}
				}
			}
			return null;
		}
	}

	public boolean filter(String filterText) {
		selectableNavigation.clear();
		if (filterText == null) {
			filterText = lastFilterText;
		} else {
			lastFilterText = filterText;
		}
		filterText = filterText.toLowerCase();
		HashSet okChar = new HashSet<String>();
		boolean b = false;
		for (Label l : groupCaptions) {
			l.setVisible(filterText.length() == 0);
		}
		for (int i = 0; i < fp.getWidgetCount(); i++) {
			Widget widget = fp.getWidget(i);
			if (widget instanceof VisualFilterable) {
				VisualFilterable td = (VisualFilterable) widget;
				boolean r = td.filter(filterText);
				b |= td.filter(filterText);
			}
		}
		return b;
	}

	public ClickHandler getEnterHandler() {
		return enterHandler;
	}

	public FilterWidget getFilter() {
		return this.filter;
	}

	public String getHint() {
		return hint;
	}

	public String getInPanelHint() {
		return inPanelHint;
	}

	public Map<G, List<T>> getItemMap() {
		return itemMap;
	}

	public List<G> getKeys() {
		return this.keys;
	}

	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return Arrays.asList(new Widget[] { focusPanel, holder })
						.iterator();
			}
		};
	}

	public ScrollPanel getScroller() {
		return this.scroller;
	}

	public Set getSelectedItems() {
		return selectedItems;
	}

	public String getSeparatorText() {
		return separatorText;
	}

	public int getTopAdjust() {
		return topAdjust;
	}

	public boolean isFlowLayout() {
		return flowLayout;
	}

	public boolean isFocusOnAttach() {
		return focusOnAttach;
	}

	public boolean isPopdown() {
		return popdown;
	}

	public boolean isSortGroupContents() {
		return sortGroupContents;
	}

	public boolean isSortGroups() {
		return sortGroups;
	}

	public void onFocus(FocusEvent event) {
		Widget sender = (Widget) event.getSource();
		int i = 0;
		log("gained focus, flowPanelForFocusPanel vis:"
				+ flowPanelForFocusPanel.isVisible() + "-widget:"
				+ sender.getClass().getName(), null);
		checkShowPopup();
		waitingToFocus = true;
		new Timer() {
			@Override
			public void run() {
				log("focus timer", null);
				filter.getTextBox().setFocus(true);
				waitingToFocus = false;
			}
		}.schedule(350);
	}

	public void onBlur(BlurEvent event) {
		log("lost focus, fp vis:" + flowPanelForFocusPanel.isVisible()
				+ "-widget:" + event.getSource().getClass().getName(), null);
		if (!waitingToFocus) {
			new Timer() {
				@Override
				public void run() {
					log("lost focus timer", null);
					if (!waitingToFocus) {
						log("not waiting - lost focus-butc", null);
						flowPanelForFocusPanel.setVisible(false);
					}
				}
			}.schedule(250);
		}
	}

	public void setAbsoluteContainers(Widget[] widgets) {
		this.absoluteContainers = widgets;
	}

	public void setEnterHandler(ClickHandler enterHandler) {
		this.enterHandler = enterHandler;
	}

	public void setFlowLayout(boolean flowLayout) {
		this.flowLayout = flowLayout;
	}

	public void setFocusOnAttach(boolean focusOnAttach) {
		this.focusOnAttach = focusOnAttach;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public void setHolderHeight(String holderHeight) {
		this.holderHeight = holderHeight;
	}

	public void setInPanelHint(String inPanelHint) {
		this.inPanelHint = inPanelHint;
	}

	public void setItemMap(Map<G, List<T>> itemMap) {
		this.itemMap = itemMap;
		if (isSortGroupContents()) {
			for (List<T> ttl : itemMap.values()) {
				Collections.sort(ttl);
			}
		}
		if (isSortGroups()) {
			keys = new ArrayList<G>(itemMap.keySet());
			Collections.sort(keys);
		}
		updateItems();
	}

	public void setItemsHaveLinefeeds(boolean itemsHaveLinefeeds) {
		this.itemsHaveLinefeeds = itemsHaveLinefeeds;
	}

	public void setKeys(List<G> keys) {
		this.keys = keys;
	}

	public void setPopdown(boolean popdown) {
		this.popdown = popdown;
	}

	public void setSeparatorText(String separatorText) {
		this.separatorText = separatorText;
	}

	public void setSortGroupContents(boolean sortGroupContents) {
		this.sortGroupContents = sortGroupContents;
	}

	public void setSortGroups(boolean sortGroups) {
		this.sortGroups = sortGroups;
	}

	public void setTopAdjust(int topAdjust) {
		this.topAdjust = topAdjust;
	}

	private void updateItems() {
		fp.clear();
		if (hintLabel != null) {
			fp.add(hintLabel);
		}
		for (G c : keys) {
			if (!itemMap.containsKey(c)) {
				continue;
			}
			Label l = new Label(c.toString().toUpperCase());
			l.setStyleName("group-heading");
			groupCaptions.add(l);
			fp.add(l);
			int ctr = itemMap.get(c).size();
			for (T item : itemMap.get(c)) {
				String sep = (--ctr != 0 && separatorText.length() != 1) ? separatorText
						: "";
				HasClickHandlers hch = itemsHaveLinefeeds ? new SelectWithSearchItemDiv(
						item, false, charWidth, itemsHaveLinefeeds, l, sep)
						: new SelectWithSearchItem(item, false, charWidth,
								itemsHaveLinefeeds, l, sep);
				hch.addClickHandler(clickHandler);
				if (popdown) {
					hch.addClickHandler(popdownHider);
				}
				fp.add((Widget) hch);
				if (ctr != 0 && sep.length() == 0) {
					fp.add(new InlineHTML(" "));
				}
			}
		}
	}

	// TODO:hcdim
	void checkShowPopup() {
		if (!flowPanelForFocusPanel.isVisible()
				&& !closingOnClick
				&& System.currentTimeMillis() - lastClosingClickMillis > DELAY_TO_CHECK_FOR_CLOSING) {
			log("running check show popup", null);
			if (lazyProvider != null) {
				LazyData lazyData = lazyProvider.dataRequired();
				if (lazyData != null) {
					setKeys(lazyData.keys);
					setItemMap(lazyData.data);
				}
			}
			flowPanelForFocusPanel.setHeight("12em");
			flowPanelForFocusPanel.setPosition(200, 200);
			filter(filter.getTextBox().getText());
			flowPanelForFocusPanel.setVisible(true);
		}
	}

	void log(String t, Throwable e) {
		// JadeClient.theApp.log(t);
		// GWT.log(t, e);
	}

	public void setShowHintStrategy(ShowHintStrategy showHintStrategy) {
		this.showHintStrategy = showHintStrategy;
	}

	public ShowHintStrategy getShowHintStrategy() {
		return showHintStrategy;
	}

	public static interface HasItem<T> {
		public T getItem();
	}

	public static class LazyData<G, T> {
		public Map<G, List<T>> data;

		public List<G> keys;
	}

	public interface LazyDataProvider<G, T> {
		LazyData dataRequired();
	}

	public class SelectWithSearchItem extends Link implements VisualFilterable,
			HasItem<T> {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		public SelectWithSearchItem(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			super(item.toString() + sep, asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = CommonUtils.singularOrPluralToString(item) + sep;
			filterableText = text.toLowerCase();
			if (text.length() < charWidth) {
				setHTML("<span style='white-space:nowrap'>" + text + "</span> ");
			} else {
				setHTML("<br />" + text + "<br />");
			}
			setStyleName("chooser-item");
		}

		public boolean filter(String filterText) {
			boolean b = filterableText.contains(filterText)
					&& !selectedItems.contains(item);
			setVisible(b);
			if (b && !ownerLabel.isVisible()) {
				ownerLabel.setVisible(true);
			}
			return b;
		}

		public T getItem() {
			return item;
		}
	}

	public class SelectWithSearchItemDiv extends BlockLink implements
			VisualFilterable, HasItem<T> {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		public SelectWithSearchItemDiv(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			super(item.toString(), asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = CommonUtils.singularOrPluralToString(item);
			filterableText = text.toLowerCase();
			setHTML(text + sep);
			setStyleName("chooser-item");
		}

		public boolean filter(String filterText) {
			boolean b = filterableText.contains(filterText)
					&& !selectedItems.contains(item);
			setVisible(b);
			if (b && !ownerLabel.isVisible()) {
				ownerLabel.setVisible(true);
			}
			return b;
		}

		public T getItem() {
			return item;
		}
	}

	public abstract static class ShowHintStrategy {
		protected FilterWidget filterWidget;

		protected Widget hintWidget;

		public void registerFilter(FilterWidget filterWidget) {
			this.filterWidget = filterWidget;
		}

		public void registerHintWidget(Widget hintWidget) {
			this.hintWidget = hintWidget;
		}
	}

	class PositionablePopDown extends FlowPanel {
		public void setPopupPosition(int left, int top) {
			if (left < 0) {
				left = 0;
			}
			if (top < 0) {
				top = 0;
			}
			left -= Document.get().getBodyOffsetLeft();
			top -= Document.get().getBodyOffsetTop();
			Element elem = getElement();
			DOM.setStyleAttribute(elem, "left", left + "px");
			DOM.setStyleAttribute(elem, "top", top + "px");
		}

		public void setPosition(int offsetWidth, int offsetHeight) {
			TextBox box = filter.getTextBox();
			int textBoxOffsetWidth = box.getOffsetWidth();
			int offsetWidthDiff = offsetWidth - textBoxOffsetWidth;
			int left;
			left = box.getAbsoluteLeft();
			if (absoluteContainers != null) {
				for (Widget w : absoluteContainers) {
					left -= w.getAbsoluteLeft();
				}
			}
			if (offsetWidthDiff > 0) {
				int windowRight = Window.getClientWidth()
						+ Window.getScrollLeft();
				int windowLeft = Window.getScrollLeft();
				int distanceToWindowRight = windowRight - left;
				int distanceFromWindowLeft = left - windowLeft;
				if (distanceToWindowRight < offsetWidth
						&& distanceFromWindowLeft >= offsetWidthDiff
						&& absoluteContainers == null) {
					left -= offsetWidthDiff;
				}
			}
			int top = box.getAbsoluteTop();
			if (absoluteContainers != null) {
				for (Widget w : absoluteContainers) {
					top -= w.getAbsoluteTop();
				}
				top += getTopAdjust();
			}
			int windowTop = Window.getScrollTop();
			int windowBottom = Window.getScrollTop() + Window.getClientHeight();
			int distanceFromWindowTop = top - windowTop;
			int distanceToWindowBottom = windowBottom
					- (top + box.getOffsetHeight());
			if (distanceToWindowBottom < offsetHeight
					&& distanceFromWindowTop >= offsetHeight
					&& absoluteContainers == null) {
				top -= offsetHeight;
			} else {
				top += box.getOffsetHeight();
			}
			setPopupPosition(left, top);
			setWidth(offsetWidth + "px");
			setHeight(offsetHeight + "px");
		}
	}

	public void clearFilterText() {
		getFilter().getTextBox().setText("");
		selectableNavigation.clear();
		filter("");
	}
}