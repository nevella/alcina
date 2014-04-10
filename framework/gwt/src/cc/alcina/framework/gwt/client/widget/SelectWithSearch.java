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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
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
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IndexedPanel;
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
public class SelectWithSearch<G, T> implements VisualFilterable, FocusHandler,
		HasLayoutInfo {
	public static final ClickHandler NOOP_CLICK_HANDLER = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			// ignore
		}
	};

	private static final int DELAY_TO_CHECK_FOR_CLOSING = 400;

	public static Map<String, List> emptyItems() {
		HashMap<String, List> map = new HashMap<String, List>();
		map.put("", new ArrayList());
		return map;
	}

	private FlowPanel holder;

	protected Widget itemHolder;

	protected HasWidgets itemHolderAsHasWidgets() {
		return (HasWidgets) itemHolder;
	}

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

	public void setLazyProvider(LazyDataProvider<G, T> lazyProvider) {
		this.lazyProvider = lazyProvider;
	}

	private int topAdjust = 0;

	private String inPanelHint = null;

	private String lastFilterText = "";

	private Label hintLabel;

	private boolean focusOnAttach = false;

	private String separatorText = " ";

	private ShowHintStrategy showHintStrategy;

	private String popupPanelCssClassName = "noBorder";

	private boolean autoselectFirst = false;

	private Handler filterAttachHandler = new Handler() {
		@Override
		public void onAttachOrDetach(AttachEvent event) {
			if (!event.isAttached()) {
				hidePopdown();
			}
		}
	};

	protected long ignoreNextBlur = 0;

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
		filter.addAttachHandler(filterAttachHandler);
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
		createItemHolder();
		if (inPanelHint != null) {
			hintLabel = new HTML(inPanelHint);
			hintLabel.setStyleName("hint");
			if (showHintStrategy != null) {
				showHintStrategy.registerHintWidget(hintLabel);
				showHintStrategy.registerFilter(filter);
			}
			itemHolderAsHasWidgets().add(hintLabel);
		}
		groupCaptions = new ArrayList<Label>();
		popdownHider = new ClickHandler() {
			public void onClick(ClickEvent event) {
				maybeClosePopdown(event);
			}
		};
		filter.getTextBox().addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				System.out.println("onblur - ignore:" + ignoreNextBlur);
				if (System.currentTimeMillis() - ignoreNextBlur < 100) {
					ignoreNextBlur = 0;
					filter.getTextBox().setFocus(true);
				} else {
					handleFilterBlur();
				}
			}
		});
		if (itemMap != null) {
			setItemMap(itemMap);
		}
		this.scroller = isFlowLayout() ? new ScrollPanel(itemHolder)
				: new ScrollPanel100pcHeight(itemHolder);
		if (!isFlowLayout()) {
			scroller.setSize("100%", "100%");
		}
		holder.setStyleName("alcina-Chooser");
		holder.add(filter);
		if (popdown) {
			panelForPopup = new DecoratedRelativePopupPanel(true);
			setPanelForPopupUI(panelForPopup);
			panelForPopup.add(scroller);
			filter.getTextBox().addFocusHandler(this);
			filter.getTextBox().addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					checkShowPopup();
				}
			});
			filter.getTextBox().addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					if (Event.getCurrentEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
						if (popdown) {
							maybeClosePopdown(null);
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

	protected void handleFilterBlur() {
		new Timer() {
			@Override
			public void run() {
				// https://jira.barnet.com.au/browse/JAD-5053 - IE
				// blur/scrollbar issue
				if (BrowserMod.isInternetExplorer()) {
					Element elt = WidgetUtils.getFocussedDocumentElement();
					if (elt != null && elt.getClassName().contains("scroller")) {
						return;
					}
				}
				hidePopdown();
			}
		}.schedule(250);
	}

	protected void onPopdownShowing(RelativePopupPanel popup, boolean show) {
	}

	protected void maybeClosePopdown(ClickEvent event) {
		if (event != null) {
			try {
				if (WidgetUtils.isNewTabModifier() || event.isShiftKeyDown()) {
					event.preventDefault();
					ignoreNextBlur = System.currentTimeMillis();
					// otherwise popup will be closed by blur
					return;
				}
			} catch (Exception e) {
				// probably a synth click
			}
		}
		closingOnClick = true;
		if (relativePopupPanel != null) {
			onPopdownShowing(relativePopupPanel, false);
			relativePopupPanel.removeFromParent();
			relativePopupPanel=null;
		}
		lastClosingClickMillis = System.currentTimeMillis();
		closingOnClick = false;
	}

	protected void setPanelForPopupUI(DecoratedRelativePopupPanel panelForPopup) {
		panelForPopup.setStyleName("dropdown-popup");
		panelForPopup.addStyleName("alcina-Selector");
		panelForPopup.getElement().getStyle()
				.setProperty("maxHeight", holderHeight);
	}

	protected void createItemHolder() {
		FlowPanelClickable panel = new FlowPanelClickable();
		panel.setStyleName("select-item-container");
		if (popdown) {
			panel.addMouseDownHandler(checkIgnoreHandler);
			panel.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					int debug = 5;
				}
			});
		}
		itemHolder = panel;
	}

	private MouseDownHandler checkIgnoreHandler = new MouseDownHandler() {
		@Override
		public void onMouseDown(MouseDownEvent event) {
			if (WidgetUtils.isNewTabModifier() || event.isShiftKeyDown()) {
				ignoreNextBlur = System.currentTimeMillis();
				System.out.println("mouse shift - ignore:" + ignoreNextBlur);
				// otherwise popup will be closed by blur
				return;
			}
		}
	};

	public void hidePopdown() {
		if (popdownHider != null) {
			maybeClosePopdown(null);
		}
	}

	SelectableNavigation selectableNavigation = new SelectableNavigation();

	protected RelativePopupPanel relativePopupPanel;

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
				maybeClosePopdown(null);
			}
		}

		private int getVisibleFilterableCount() {
			int visibleIndex = -1;
			IndexedPanel itemHolder = itemHolderAsIndexedPanel();
			for (int i = 0; i < itemHolder.getWidgetCount(); i++) {
				Widget widget = itemHolder.getWidget(i);
				if (widget instanceof VisualFilterable && widget.isVisible()) {
					visibleIndex++;
				}
			}
			return visibleIndex;
		}

		private Widget getSelectedWidget() {
			int visibleIndex = -1;
			IndexedPanel itemHolder = itemHolderAsIndexedPanel();
			for (int i = 0; i < itemHolder.getWidgetCount(); i++) {
				Widget widget = itemHolder.getWidget(i);
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
		IndexedPanel itemHolder = itemHolderAsIndexedPanel();
		for (int i = 0; i < itemHolder.getWidgetCount(); i++) {
			Widget widget = itemHolder.getWidget(i);
			if (widget instanceof VisualFilterable) {
				VisualFilterable td = (VisualFilterable) widget;
				boolean r = td.filter(filterText);
				b |= td.filter(filterText);
			}
		}
		return b;
	}

	public IndexedPanel itemHolderAsIndexedPanel() {
		return (IndexedPanel) itemHolder;
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
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				checkShowPopup();
				filter.getTextBox().setFocus(true);
			}
		});
	}

	public void setEnterHandler(ClickHandler enterHandler) {
		this.enterHandler = enterHandler;
	}

	public void setFlowLayout(boolean flowLayout) {
		this.flowLayout = flowLayout;
	}

	public void setFocusOnAttach(boolean focusOnAttach) {
		this.focusOnAttach = focusOnAttach;
		if (filter != null) {
			filter.setFocusOnAttach(focusOnAttach);
		}
	}

	public void setHint(String hint) {
		this.hint = hint;
		if (filter != null) {
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
		selectableNavigation.clear();
		this.itemMap = itemMap;
		if (isSortGroupContents()) {
			for (List<T> ttl : itemMap.values()) {
				Collections.sort((List) ttl);
			}
		}
		if (isSortGroups()) {
			keys = new ArrayList<G>(itemMap.keySet());
			Collections.sort((List) keys);
		}
		updateItems();
		if (isAutoselectFirst()) {
			selectableNavigation.selectedIndex = 0;
			selectableNavigation.updateSelection();
		}
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

	protected void updateItems() {
		HasWidgets itemHolder = itemHolderAsHasWidgets();
		itemHolder.clear();
		emptyItems = true;
		if (hintLabel != null) {
			itemHolder.add(hintLabel);
			emptyItems = false;
		}
		for (G c : keys) {
			if (!itemMap.containsKey(c)) {
				continue;
			}
			Label l = new Label(c.toString().toUpperCase());
			l.setStyleName("group-heading");
			groupCaptions.add(l);
			addGroupHeading(itemHolder, l);
			if (c.toString().trim().isEmpty()) {
				l.getElement().getStyle().setVisibility(Visibility.HIDDEN);
			} else {
				emptyItems = false;
			}
			int ctr = itemMap.get(c).size();
			for (T item : itemMap.get(c)) {
				emptyItems = false;
				String sep = (--ctr != 0 && separatorText.length() != 1) ? separatorText
						: "";
				HasClickHandlers hch = createItem(item, false, charWidth,
						itemsHaveLinefeeds, l, sep);
				hch.addClickHandler(clickHandler);
				if (popdown) {
					hch.addClickHandler(popdownHider);
				}
				itemHolder.add((Widget) hch);
				if (ctr != 0 && sep.length() == 0) {
					addDefaultSeparator(itemHolder);
				}
			}
		}
		afterUpdateItems(emptyItems);
	}

	protected void addDefaultSeparator(HasWidgets itemHolder) {
		itemHolder.add(new InlineHTML(" "));
	}

	protected void addGroupHeading(HasWidgets itemHolder, Label l) {
		itemHolder.add(l);
	}

	boolean emptyItems = false;

	protected void afterUpdateItems(boolean empty) {
	}

	protected HasClickHandlers createItem(T item, boolean asHTML,
			int charWidth, boolean itemsHaveLinefeeds, Label ownerLabel,
			String sep) {
		HasClickHandlers hch = itemsHaveLinefeeds ? new SelectWithSearchItemDiv(
				item, false, charWidth, itemsHaveLinefeeds, ownerLabel, sep)
				: new SelectWithSearchItem(item, false, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep);
		return hch;
	}

	protected int shiftY() {
		return 0;
	}

	protected void checkShowPopup() {
		checkShowPopup(true);
	}

	// TODO:hcdim
	protected void checkShowPopup(boolean filterTextBox) {
		if ((this.relativePopupPanel == null || this.relativePopupPanel
				.getParent() == null)
				&& !closingOnClick
				&& System.currentTimeMillis() - lastClosingClickMillis > DELAY_TO_CHECK_FOR_CLOSING
				&& maybeShowDepdendentOnFilter()) {
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
			if (filterTextBox && !filter.isQueueing()) {
				filter(filter.getTextBox().getText());
			}
			this.relativePopupPanel = RelativePopupPositioning
					.showPopup(
							filter,
							null,
							RootPanel.get(),
							new RelativePopupAxis[] { RelativePopupPositioning.BOTTOM_LTR },
							RootPanel.get(), panelForPopup, shiftX(), shiftY());
			onPopdownShowing(relativePopupPanel, true);
			int border = 2;
			if (itemHolder.getOffsetHeight() + border > panelForPopup
					.getOffsetHeight() && !isAutoHolderHeight()) {
				int hhInt = holderHeight != null && holderHeight.endsWith("px") ? Integer
						.parseInt(holderHeight.replace("px", "")) : 0;
				scroller.setHeight(Math.max(hhInt,
						panelForPopup.getOffsetHeight() - border)
						+ "px");
			}
			int minWidth = holder.getOffsetWidth();
			if (minWidth == 0) {// probably inline
				minWidth = filter.getOffsetWidth();
			}
			minWidth = adjustDropdownWidth(minWidth);
			if (minWidth > 20) {
				scroller.getElement().getStyle()
						.setProperty("minWidth", minWidth + "px");
				if (BrowserMod.isIEpre9()) {
					relativePopupPanel.getElement().getStyle()
							.setProperty("minWidth", (minWidth + 20) + "px");
				}
			}
			afterUpdateItems(emptyItems);
		}
	}

	protected boolean isAutoHolderHeight() {
		return false;
	}

	protected boolean maybeShowDepdendentOnFilter() {
		return true;
	}

	protected int shiftX() {
		return -2;
	}

	protected int adjustDropdownWidth(int minWidth) {
		return minWidth;
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
			String text = (String) renderer.render(item);
			Label label = asHTML ? new InlineHTML(text) : new InlineLabel(text);
			add(label);
			label.setStyleName("text");
			this.item = item;
			this.ownerLabel = ownerLabel;
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

	public static class HideOnKeypressHintStrategy extends ShowHintStrategy
			implements KeyDownHandler {
		private boolean hintShown = false;

		@Override
		public void registerFilter(FilterWidget filter) {
			super.registerFilter(filter);
			filter.getTextBox().addKeyDownHandler(this);
		}

		@Override
		public void registerHintWidget(Widget hintWidget) {
			super.registerHintWidget(hintWidget);
			if (hintShown) {
				hintWidget.setVisible(false);
			}
		}

		public void onKeyDown(KeyDownEvent event) {
			hintShown = true;
			hintWidget.setVisible(false);
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

	public void setAutoselectFirst(boolean autoselectFirst) {
		this.autoselectFirst = autoselectFirst;
	}

	public boolean isAutoselectFirst() {
		return autoselectFirst;
	}

	public void maybeRepositionPopdown() {
		if (relativePopupPanel != null
				&& WidgetUtils.isVisibleAncestorChain(relativePopupPanel)) {
			RelativePopupPositioning
					.showPopup(
							filter,
							null,
							RootPanel.get(),
							new RelativePopupAxis[] { RelativePopupPositioning.BOTTOM_LTR },
							RootPanel.get(), panelForPopup, shiftX(), shiftY());
		}
	}
}