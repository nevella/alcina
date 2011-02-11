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

import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight;

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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

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

	private DecoratedRelativePopupPanel panelForPopup;

	private boolean closingOnClick = false;

	private Set selectedItems = new HashSet();

	private long lastClosingClickMillis;// ie doesn't do these linearly -

	private String popdownStyleName;

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

	private ShowHintStrategy showHintStrategy;

	private String popupPanelCssClassName = "noBorder";

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
				if (relativePopupPanel != null) {
					relativePopupPanel.hide();
				}
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
		}
		holder.setStyleName("alcina-Chooser");
		holder.add(filter);
		if (popdown) {
			panelForPopup = new DecoratedRelativePopupPanel(true);
			panelForPopup.setStyleName("dropdown-popup");
			panelForPopup.addStyleName("alcina-Selector");
			panelForPopup.getElement().getStyle()
					.setProperty("maxHeight", holderHeight);
			panelForPopup.add(scroller);
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
		} else {
			if (!isFlowLayout()) {
				holder.setHeight(holderHeight);
			}
			holder.add(scroller);
		}
		return holder;
	}

	SelectableNavigation selectableNavigation = new SelectableNavigation();

	private RelativePopupPanel relativePopupPanel;

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
						WidgetUtils.fireClickOnHandler(
								(HasClickHandlers) event.getSource(),
								wrappedEnterListener);
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
				return Arrays.asList(
						popdown ? new Widget[] {} : new Widget[] { focusPanel,
								holder }).iterator();
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
		// log("gained focus, flowPanelForFocusPanel vis:"
		// + panelForPopup.isVisible() + "-widget:"
		// + sender.getClass().getName(), null);
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
		// log("lost focus, fp vis:" + panelForPopup.isVisible() + "-widget:"
		// + event.getSource().getClass().getName(), null);
		if (!waitingToFocus) {
			new Timer() {
				@Override
				public void run() {
					// log("lost focus timer", null);
					if (!waitingToFocus) {
						// log("not waiting - lost focus-butc", null);
						if ((relativePopupPanel == null || relativePopupPanel
								.getParent() == null)) {
							relativePopupPanel.hide();
						}
					}
				}
			}.schedule(250);
		}
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
		if(filter!=null){
			filter.setHint(hint);
		}
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
				HasClickHandlers hch = createItem(item, false, charWidth,
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

	public HasClickHandlers createItem(T item, boolean asHTML, int charWidth,
			boolean itemsHaveLinefeeds, Label ownerLabel, String sep) {
		HasClickHandlers hch = itemsHaveLinefeeds ? new SelectWithSearchItemDiv(
				item, false, charWidth, itemsHaveLinefeeds, ownerLabel, sep)
				: new SelectWithSearchItem(item, false, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep);
		return hch;
	}

	// TODO:hcdim
	void checkShowPopup() {
		if ((this.relativePopupPanel == null || this.relativePopupPanel
				.getParent() == null)
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
			if (popdownStyleName != null) {
				panelForPopup.addStyleName(popdownStyleName);
			}
			filter(filter.getTextBox().getText());
			this.relativePopupPanel = RelativePopupPositioning
					.showPopup(
							filter,
							null,
							RootPanel.get(),
							new RelativePopupAxis[] { RelativePopupPositioning.BOTTOM_LTR },
							RootPanel.get(), panelForPopup, -2, 0);
			int border = 2;
			if (fp.getOffsetHeight() + border > panelForPopup.getOffsetHeight()) {
				scroller.setHeight((panelForPopup.getOffsetHeight() - border)
						+ "px");
			}
			int minWidth = holder.getOffsetWidth();
			if (minWidth > 20) {
				scroller.getElement().getStyle()
						.setProperty("minWidth", minWidth + "px");
			}
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

	public class SelectWithSearchItem extends Link implements VisualFilterable {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		public SelectWithSearchItem(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			super(item.toString() + sep, asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = (String) renderer.render(item) + sep;
			filterableText = text.toLowerCase();
			// if (text.length() < charWidth) {
			// this is just too hacky - use mouseover highlight to differentiate
			// instead
			setHTML("<span style='white-space:nowrap'>" + text + "</span> ");
			// } else {
			// setHTML("<br />" + text + "<br />");
			// }
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

	public class SelectWithSearchItemX extends SpanPanel implements
			VisualFilterable, HasItem<T>, HasClickHandlers {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		private Link hl;

		public SelectWithSearchItemX(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			Label label = asHTML ? new InlineHTML(item.toString())
					: new InlineLabel(item.toString());
			add(label);
			label.setStyleName("text");
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = (String) renderer.render(item);
			filterableText = text.toLowerCase();
			AbstractImagePrototype aip = AbstractImagePrototype
					.create(StandardDataImageProvider.get().getDataImages()
							.deleteItem());
			hl = new Link(aip.getHTML(), true);
			hl.setUserObject(item);
			add(label);
			add(hl);
			setStyleName("selectx");
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

		@Override
		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return hl.addClickHandler(handler);
		}
	}

	private Renderer renderer = ToStringRenderer.INSTANCE;

	public Renderer getRenderer() {
		return this.renderer;
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public class SelectWithSearchItemDiv extends BlockLink implements
			VisualFilterable {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		public SelectWithSearchItemDiv(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			super(item.toString(), asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = (String) renderer.render(item);
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

	public void clearFilterText() {
		getFilter().getTextBox().setText("");
		selectableNavigation.clear();
		filter("");
	}

	public void setPopdownStyleName(String popdownStyleName) {
		this.popdownStyleName = popdownStyleName;
	}

	public String getPopdownStyleName() {
		return popdownStyleName;
	}

	public void removeScroller() {
		Widget child = scroller.getWidget();
		holder.remove(scroller);
		holder.add(child);
	}

	public void setPopupPanelCssClassName(String popupPanelCssClassName) {
		this.popupPanelCssClassName = popupPanelCssClassName;
	}

	public String getPopupPanelCssClassName() {
		return popupPanelCssClassName;
	}
}