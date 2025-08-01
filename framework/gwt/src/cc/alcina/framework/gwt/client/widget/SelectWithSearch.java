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
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
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
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SimpleKeyProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.PopupShownEvent.HasPopupShownHandlers;
import cc.alcina.framework.gwt.client.widget.PopupShownEvent.PopupShownHandler;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight.ScrollPanel100pcHeight300px;

@SuppressWarnings({ "unchecked", "deprecation" })
/**
 *
 * @author Nick Reddel
 */
public class SelectWithSearch<G, T> implements VisualFilterable, FocusHandler,
		HasLayoutInfo, HasSelectionHandlers<T>, HasPopupShownHandlers {
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
			if (!event.isAttached() && !showFilterInPopup) {
				hidePopdown();
			}
		}
	};

	protected long ignoreNextBlur = 0;

	private String initialFilterValue = null;

	private int initialFilterCursorPos = 0;

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

	SelectableNavigation selectableNavigation = new SelectableNavigation();

	protected RelativePopupPanel relativePopupPanel;

	boolean emptyItems = false;

	private Function renderer = ToStringRenderer.INSTANCE;

	private VisualFilterableItemFilter<T> itemFilter = new VisualFilterableItemFilter<T>();

	private boolean useCellList = false;

	HandlerManager handlerManager = new HandlerManager(this);

	private boolean showFilterInPopup = false;

	private boolean showSelectedItemsInSearch;

	// use when showing filter in popup (ie totally hidden)
	private Supplier<Widget> showFilterRelativeTo;

	private boolean closeOnPopdownFilterEmpty = true;

	private int shiftX;

	private boolean recreateItemHolderOnRefresh;

	private String emptyItemsText;

	private boolean matchWidthToSource;

	private int shiftY;

	public SelectWithSearch() {
	}

	protected void addDefaultSeparator(HasWidgets itemHolder) {
		itemHolder.add(new InlineHTML(" "));
	}

	protected void addGroupHeading(HasWidgets itemHolder, Label l) {
		itemHolder.add(l);
	}

	@Override
	public HandlerRegistration addPopupShownHandler(PopupShownHandler handler) {
		return handlerManager.addHandler(PopupShownEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration
			addSelectionHandler(SelectionHandler<T> handler) {
		return handlerManager.addHandler(SelectionEvent.getType(), handler);
	}

	public HandlerRegistration addWidgetClickHandler(ClickHandler handler) {
		return ((HasClickHandlers) holder).addClickHandler(handler);
	}

	protected int adjustDropdownWidth(int minWidth) {
		return minWidth;
	}

	protected void afterUpdateItems(boolean empty) {
		holder.setStyleName("has-items", !empty);
	}

	protected void checkShowPopup() {
		checkShowPopup(true);
	}

	public void checkShowPopup(final boolean filterTextBox) {
		if ((this.relativePopupPanel == null
				|| this.relativePopupPanel.getParent() == null)
				&& !closingOnClick
				&& System.currentTimeMillis()
						- lastClosingClickMillis > DELAY_TO_CHECK_FOR_CLOSING
				&& maybeShowDepdendentOnFilter()) {
			if (lazyProvider != null) {
				AsyncCallback<LazyData> callback = new AsyncCallbackStd<SelectWithSearch.LazyData>() {
					@Override
					public void onSuccess(LazyData lazyData) {
						if (lazyData != null) {
							setKeys(lazyData.keys);
							setItemMap(lazyData.data);
						}
						showPopupWithData(filterTextBox);
					}
				};
				lazyProvider.getData(callback);
			} else {
				showPopupWithData(filterTextBox);
			}
		}
	}

	public void clearFilterText() {
		getFilter().getTextBox().setText("");
		selectableNavigation.clear();
		filter("");
	}

	protected HasClickHandlers createItem(T item, boolean asHTML, int charWidth,
			boolean itemsHaveLinefeeds, Label ownerLabel, String sep) {
		HasClickHandlers hch = itemsHaveLinefeeds
				? new SelectWithSearchItemDiv(item, false, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep, itemFilter)
				: new SelectWithSearchItem(item, false, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep);
		return hch;
	}

	protected void createItemHolder() {
		FlowPanelClickable panel = new FlowPanelClickable();
		panel.setStyleName("select-item-container");
		if (popdown) {
			panel.addMouseDownHandler(checkIgnoreHandler);
		}
		itemHolder = panel;
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
		this.holder = isFlowLayout() ? new FlowPanelClickable()
				: new FlowPanel100pcHeight();
		filter = new FilterWidget(hint);
		filter.getTextBox().addKeyUpHandler(selectableNavigation);
		filter.getTextBox().addKeyDownHandler(selectableNavigation);
		if (getInitialFilterValue() != null) {
			filter.setInitialCursorPos(getInitialFilterCursorPos());
			filter.setValue(getInitialFilterValue());
		}
		filter.setFocusOnAttach(isFocusOnAttach());
		filter.addAttachHandler(filterAttachHandler);
		filter.registerFilterable(this);
		selectableNavigation.setWrappedEnterListener(new ClickHandler() {
			// the listeners aren't registered on every source...pretty sure
			// this is logical...
			@Override
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
			@Override
			public void onClick(ClickEvent event) {
				maybeClosePopdown(event);
			}
		};
		filter.getTextBox().addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if ("".isEmpty()) {
					return;
				}
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
				: new ScrollPanel100pcHeight300px(itemHolder);
		if (!isFlowLayout()) {
			scroller.setSize("100%", "100%");
		}
		scroller.setStyleName("selector-scroller");
		holder.setStyleName("alcina-Chooser");
		holder.add(filter);
		if (popdown) {
			filter.getTextBox().addFocusHandler(this);
			filter.getTextBox().addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					checkShowPopup();
				}
			});
			filter.getTextBox().addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent event) {
					if (Event.getCurrentEvent()
							.getKeyCode() == KeyCodes.KEY_ESCAPE) {
						if (popdown) {
							maybeClosePopdown(null);
						}
					} else {
						checkShowPopup();
					}
					if (CommonUtils.isNullOrEmpty(filter.getTextBox().getText())
							&& popdown && isCloseOnPopdownFilterEmpty()) {
						maybeClosePopdown(null);
					}
				}
			});
		} else {
			if (!isFlowLayout()) {
				holder.setHeight(holderHeight);
			}
			holder.add(scroller);
		}
		if (!popdown && lazyProvider != null) {
			AsyncCallback<LazyData> callback = new AsyncCallbackStd<SelectWithSearch.LazyData>() {
				@Override
				public void onSuccess(LazyData lazyData) {
					if (lazyData != null) {
						setKeys(lazyData.keys);
						setItemMap(lazyData.data);
					}
				}
			};
			lazyProvider.getData(callback);
		}
		return holder;
	}

	private DecoratedRelativePopupPanel ensurePanelForPopup() {
		if (panelForPopup == null) {
			panelForPopup = new DecoratedRelativePopupPanel(true);
			setPanelForPopupUI(panelForPopup);
			panelForPopup.add(scroller);
		}
		return panelForPopup;
	}

	@Override
	public boolean filter(String filterText) {
		selectableNavigation.clear();
		if (filterText == null) {
			filterText = lastFilterText;
		} else {
			lastFilterText = filterText;
		}
		if (isUseCellList()) {
			updateItemsCellList(filterText, (HasWidgets) itemHolder);
			return false;
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

	@Override
	public void fireEvent(GwtEvent<?> event) {
		handlerManager.fireEvent(event);
	}

	public String getEmptyItemsText() {
		return this.emptyItemsText;
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

	public int getInitialFilterCursorPos() {
		return this.initialFilterCursorPos;
	}

	public String getInitialFilterValue() {
		return this.initialFilterValue;
	}

	public String getInPanelHint() {
		return inPanelHint;
	}

	public VisualFilterableItemFilter<T> getItemFilter() {
		return this.itemFilter;
	}

	public Map<G, List<T>> getItemMap() {
		return itemMap;
	}

	public List<G> getKeys() {
		return this.keys;
	}

	@Override
	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return Arrays
						.asList(popdown ? new Widget[] {}
								: new Widget[] { focusPanel, holder })
						.iterator();
			}
		};
	}

	public LazyDataProvider<G, T> getLazyProvider() {
		return this.lazyProvider;
	}

	public String getPopdownStyleName() {
		return popdownStyleName;
	}

	public String getPopupPanelCssClassName() {
		return popupPanelCssClassName;
	}

	public Function getRenderer() {
		return this.renderer;
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

	public int getShiftX() {
		return shiftX;
	}

	public int getShiftY() {
		return this.shiftY;
	}

	public Supplier<Widget> getShowFilterRelativeTo() {
		return this.showFilterRelativeTo;
	}

	public ShowHintStrategy getShowHintStrategy() {
		return showHintStrategy;
	}

	public int getTopAdjust() {
		return topAdjust;
	}

	protected void handleFilterBlur() {
		new Timer() {
			@Override
			public void run() {
				// https://jira.barnet.com.au/browse/JAD-5053 - IE
				// blur/scrollbar issue
				if (BrowserMod.isInternetExplorer()) {
					Element elt = WidgetUtils.getFocussedDocumentElement();
					if (elt != null
							&& elt.getClassName().contains("scroller")) {
						return;
					}
				}
				hidePopdown();
			}
		}.schedule(250);
	}

	public void hidePopdown() {
		if (popdownHider != null) {
			maybeClosePopdown(null);
		}
	}

	protected boolean isAutoHolderHeight() {
		return false;
	}

	public boolean isAutoselectFirst() {
		return autoselectFirst;
	}

	public boolean isCloseOnPopdownFilterEmpty() {
		return this.closeOnPopdownFilterEmpty;
	}

	public boolean isFlowLayout() {
		return flowLayout;
	}

	public boolean isFocusOnAttach() {
		return focusOnAttach;
	}

	public boolean isMatchWidthToSource() {
		return this.matchWidthToSource;
	}

	public boolean isPopdown() {
		return popdown;
	}

	public boolean isRecreateItemHolderOnRefresh() {
		return this.recreateItemHolderOnRefresh;
	}

	public boolean isShowFilterInPopup() {
		return this.showFilterInPopup;
	}

	public boolean isShowingPopdown() {
		return relativePopupPanel != null
				&& WidgetUtils.isVisibleAncestorChain(relativePopupPanel);
	}

	public boolean isShowSelectedItemsInSearch() {
		return this.showSelectedItemsInSearch;
	}

	public boolean isSortGroupContents() {
		return sortGroupContents;
	}

	public boolean isSortGroups() {
		return sortGroups;
	}

	public boolean isUseCellList() {
		return this.useCellList;
	}

	protected HasWidgets itemHolderAsHasWidgets() {
		return (HasWidgets) itemHolder;
	}

	public IndexedPanel itemHolderAsIndexedPanel() {
		return (IndexedPanel) itemHolder;
	}

	protected void itemSelected(T item) {
		SelectionEvent.fire(this, item);
	}

	protected void maybeClosePopdown(ClickEvent event) {
		if (event != null) {
			try {
				if (WidgetUtils.isNewTabModifier(event.getNativeEvent())
						|| event.isShiftKeyDown()) {
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
			relativePopupPanel = null;
		}
		lastClosingClickMillis = System.currentTimeMillis();
		closingOnClick = false;
	}

	public void maybeRepositionPopdown() {
		if (relativePopupPanel != null
				&& WidgetUtils.isVisibleAncestorChain(relativePopupPanel)) {
			RelativePopupPositioning.showPopup(filter, null, RootPanel.get(),
					new RelativePopupAxis[] {
							RelativePopupPositioning.BOTTOM_LTR },
					RootPanel.get(), ensurePanelForPopup(), getShiftX(),
					shiftY());
		}
	}

	protected boolean maybeShowDepdendentOnFilter() {
		return true;
	}

	@Override
	public void onFocus(FocusEvent event) {
		Widget sender = (Widget) event.getSource();
		if (sender == filter.getTextBox()) {
			return;
		}
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				checkShowPopup();
				filter.getTextBox().setFocus(true);
			}
		});
	}

	protected void onPopdownShowing(RelativePopupPanel popup, boolean show) {
		PopupShownEvent.fire(this, show);
		holder.setStyleName("showing-popup", show);
	}

	public String provideFilterBoxText() {
		return getFilter().getTextBox().getText();
	}

	public void removeScroller() {
		Widget child = scroller.getWidget();
		holder.remove(scroller);
		holder.add(child);
	}

	public void setAutoselectFirst(boolean autoselectFirst) {
		this.autoselectFirst = autoselectFirst;
	}

	public void
			setCloseOnPopdownFilterEmpty(boolean closeOnPopdownFilterEmpty) {
		this.closeOnPopdownFilterEmpty = closeOnPopdownFilterEmpty;
	}

	public void setEmptyItemsText(String emptyItemsText) {
		this.emptyItemsText = emptyItemsText;
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

	public void setInitialFilterCursorPos(int initialFilterCursorPos) {
		this.initialFilterCursorPos = initialFilterCursorPos;
	}

	public void setInitialFilterValue(String initialFilterValue) {
		this.initialFilterValue = initialFilterValue;
	}

	public void setInPanelHint(String inPanelHint) {
		this.inPanelHint = inPanelHint;
	}

	public void setItemFilter(VisualFilterableItemFilter<T> itemFilter) {
		this.itemFilter = itemFilter;
	}

	public void setItemMap(Map<G, List<T>> itemMap) {
		selectableNavigation.clear();
		this.itemMap = itemMap;
		if (isSortGroupContents()) {
			for (List<T> ttl : itemMap.values()) {
				Collections.sort((List) ttl);
			}
		}
		if (keys == null) {
			keys = new ArrayList();
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

	public void setLazyProvider(LazyDataProvider<G, T> lazyProvider) {
		this.lazyProvider = lazyProvider;
	}

	public void setMatchWidthToSource(boolean matchWidthToSource) {
		this.matchWidthToSource = matchWidthToSource;
	}

	protected void
			setPanelForPopupUI(DecoratedRelativePopupPanel panelForPopup) {
		panelForPopup.setStyleName("dropdown-popup");
		panelForPopup.addStyleName("alcina-Selector");
		panelForPopup.getElement().getStyle().setProperty("maxHeight",
				holderHeight);
	}

	public void setPopdown(boolean popdown) {
		this.popdown = popdown;
	}

	public void setPopdownStyleName(String popdownStyleName) {
		this.popdownStyleName = popdownStyleName;
	}

	public void setPopupPanelCssClassName(String popupPanelCssClassName) {
		this.popupPanelCssClassName = popupPanelCssClassName;
	}

	public void setRecreateItemHolderOnRefresh(
			boolean recreateItemHolderOnRefresh) {
		this.recreateItemHolderOnRefresh = recreateItemHolderOnRefresh;
	}

	public void setRenderer(Function renderer) {
		this.renderer = renderer;
	}

	public void setSeparatorText(String separatorText) {
		this.separatorText = separatorText;
	}

	public void setShiftX(int shiftX) {
		this.shiftX = shiftX;
	}

	public void setShiftY(int shiftY) {
		this.shiftY = shiftY;
	}

	public void setShowFilterInPopup(boolean showFilterInPopup) {
		this.showFilterInPopup = showFilterInPopup;
	}

	public void setShowFilterRelativeTo(Supplier<Widget> showFilterRelativeTo) {
		this.showFilterRelativeTo = showFilterRelativeTo;
	}

	public void setShowHintStrategy(ShowHintStrategy showHintStrategy) {
		this.showHintStrategy = showHintStrategy;
	}

	public void
			setShowSelectedItemsInSearch(boolean showSelectedItemsInSearch) {
		this.showSelectedItemsInSearch = showSelectedItemsInSearch;
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

	public void setUseCellList(boolean useCellList) {
		this.useCellList = useCellList;
	}

	protected int shiftY() {
		return getShiftY();
	}

	public void showPopupWithData(boolean filterTextBox) {
		ensurePanelForPopup();
		if (popdownStyleName != null) {
			panelForPopup.addStyleName(popdownStyleName);
		}
		if (filterTextBox && !filter.isQueueing()) {
			filter(filter.getTextBox().getText());
		}
		if (isShowFilterInPopup() && !panelForPopup.getElement()
				.isOrHasChild(filter.getElement())) {
			FlowPanel fp = new FlowPanel();
			fp.add(filter);
			fp.add(panelForPopup.getWidget());
			panelForPopup.setWidget(fp);
		}
		if (matchWidthToSource) {
			filter.getElement().getStyle().setPropertyPx("minWidth",
					showFilterRelativeTo.get().getOffsetWidth());
		}
		this.relativePopupPanel = RelativePopupPositioning.showPopup(
				isShowFilterInPopup() ? showFilterRelativeTo.get() : filter,
				null, RootPanel.get(),
				new RelativePopupAxis[] { RelativePopupPositioning.BOTTOM_LTR },
				RootPanel.get(), panelForPopup, getShiftX(), shiftY());
		this.relativePopupPanel.addAttachHandler(e -> {
			if (!e.isAttached()) {
				onPopdownShowing(this.relativePopupPanel, false);
			}
		});
		if (isShowFilterInPopup()) {
			filter.setValue("");
			filter.getTextBox().setFocus(true);
			Scheduler.get().scheduleDeferred(() -> {
				filter.getTextBox().setFocus(true);
			});
		}
		onPopdownShowing(relativePopupPanel, true);
		int border = -2;
		// the 20 is a pad to make sure we have a reasonable scroller size in
		// edge cases
		if (itemHolder.getOffsetHeight()
				+ border > (panelForPopup.getOffsetHeight() - 20)
				&& !isAutoHolderHeight()) {
			int hhInt = 0;
			if (holderHeight != null && holderHeight.endsWith("px")) {
				// chrome exceptions with replace?
				String toParse = holderHeight.replace("px", "");
				hhInt = CommonUtils.friendlyParseInt(toParse);
				hhInt = Math.max(hhInt, 100);
			} else {
				hhInt = Window.getClientHeight() / 3;
			}
			String scrollerHeight = Math.min(hhInt,
					itemHolder.getOffsetHeight() - border) + "px";
			scroller.setHeight(scrollerHeight);
		}
		int minWidth = holder.getOffsetWidth();
		if (minWidth == 0) {// probably inline
			minWidth = filter.getOffsetWidth();
		}
		minWidth = adjustDropdownWidth(minWidth);
		if (minWidth > 20) {
			scroller.getElement().getStyle().setProperty("minWidth",
					minWidth + "px");
			if (BrowserMod.isIEpre9()) {
				relativePopupPanel.getElement().getStyle()
						.setProperty("minWidth", (minWidth + 20) + "px");
			}
		}
		afterUpdateItems(emptyItems);
	}

	protected void updateItems() {
		boolean recreateItemHolder = isRecreateItemHolderOnRefresh()
				&& itemHolder.getParent() != null;
		if (recreateItemHolder) {
			itemHolder.removeFromParent();
			createItemHolder();
			if (scroller != null && scroller.getParent() == null) {
				scroller = new ScrollPanel();
			}
		}
		HasWidgets itemHolder = itemHolderAsHasWidgets();
		itemHolder.clear();
		if (isUseCellList()) {
			updateItemsCellList("", itemHolder);
			return;
		}
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
				String sep = (--ctr != 0 && separatorText.length() != 1)
						? separatorText
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
		if (!itemHolder.iterator().hasNext() && emptyItemsText != null) {
			Label empty = new Label(emptyItemsText);
			empty.setStyleName("empty-items");
			itemHolder.add(empty);
		}
		if (recreateItemHolder) {
			scroller.setWidget(this.itemHolder);
		}
		afterUpdateItems(emptyItems);
	}

	private void updateItemsCellList(String filterText, HasWidgets itemHolder) {
		emptyItems = true;
		Cell<T> cell = new AbstractCell<T>() {
			@Override
			public void render(com.google.gwt.cell.client.Cell.Context context,
					T value, SafeHtmlBuilder sb) {
				sb.appendEscaped((String) renderer.apply(value));
			}
		};
		CellList<T> cellList = new CellList<T>(cell);
		cellList.setPageSize(9999);
		cellList.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);
		cellList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		// Add a selection model so we can select cells.
		final SingleSelectionModel<T> selectionModel = new SingleSelectionModel<T>(
				new SimpleKeyProvider<T>());
		cellList.setSelectionModel(selectionModel);
		selectionModel
				.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						itemSelected(selectionModel.getSelectedObject());
					}
				});
		List<T> items = new ArrayList<>();
		for (G c : keys) {
			if (!itemMap.containsKey(c)) {
				continue;
			}
			for (T item : itemMap.get(c)) {
				String filterable = CommonUtils
						.nullToEmpty(((String) renderer.apply(item)))
						.toLowerCase();
				if (itemFilter.test(item, filterable, filterText)
						&& !selectedItems.contains(item)) {
					items.add(item);
				}
			}
		}
		ListDataProvider<T> dataProvider = new ListDataProvider<T>();
		dataProvider.getList().addAll(items);
		dataProvider.addDataDisplay(cellList);
		emptyItems = items.isEmpty();
		itemHolder.clear();
		itemHolder.add(cellList);
		afterUpdateItems(emptyItems);
	}

	public static interface HasItem<T> {
		public T getItem();
	}

	public static class HideOnKeypressHintStrategy extends ShowHintStrategy
			implements KeyDownHandler {
		private boolean hintShown = false;

		@Override
		public void onKeyDown(KeyDownEvent event) {
			hintShown = true;
			hintWidget.setVisible(false);
		}

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
	}

	public static class LazyData<G, T> {
		public Map<G, List<T>> data;

		public List<G> keys;
	}

	public interface LazyDataProvider<G, T> {
		void getData(AsyncCallback<LazyData> callback);
	}

	class SelectableNavigation implements KeyUpHandler, KeyDownHandler {
		private int selectedIndex = -1;

		private Widget lastSelected = null;

		private ClickHandler wrappedEnterListener;

		public void clear() {
			selectedIndex = -1;
			updateSelection();
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

		public ClickHandler getWrappedEnterListener() {
			return this.wrappedEnterListener;
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			int keyCode = event.getNativeKeyCode();
			if (keyCode == KeyCodes.KEY_UP || keyCode == KeyCodes.KEY_DOWN) {
				WidgetUtils.squelchCurrentEvent();
			}
		}

		@Override
		public void onKeyUp(KeyUpEvent event) {
			Widget sender = (Widget) event.getSource();
			if (event.getNativeEvent() == null) {
				// IE9 issue
				return;
			}
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

		public void setWrappedEnterListener(ClickHandler enterListener) {
			this.wrappedEnterListener = enterListener;
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
	}

	public class SelectWithSearchItem extends Link implements VisualFilterable {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		public SelectWithSearchItem(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			super(CommonUtils.nullSafeToString(item) + sep, asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			String text = (String) renderer.apply(item) + sep;
			filterableText = text.toLowerCase();
			// if (text.length() < charWidth) {
			// this is just too hacky - use mouseover highlight to differentiate
			// instead
			setHTML("<span style='white-space:nowrap'>"
					+ SafeHtmlUtils.htmlEscape(text) + "</span>");
			// } else {
			// setHTML("<br />" + text + "<br />");
			// }
			setStyleName("chooser-item");
		}

		@Override
		public boolean filter(String filterText) {
			boolean b = filterableText.contains(filterText)
					&& !selectedItems.contains(item);
			setVisible(b);
			if (b && !ownerLabel.isVisible()) {
				ownerLabel.setVisible(true);
			}
			return b;
		}

		@Override
		public T getItem() {
			return item;
		}
	}

	public class SelectWithSearchItemDiv extends BlockLink
			implements VisualFilterable {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		private VisualFilterableItemFilter<T> filter;

		public SelectWithSearchItemDiv(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep,
				VisualFilterableItemFilter<T> filter) {
			super(item == null ? "" : item.toString(), asHTML);
			this.item = item;
			this.ownerLabel = ownerLabel;
			this.filter = filter;
			String text = (String) renderer.apply(item);
			text = SafeHtmlUtils.htmlEscape(text);
			filterableText = text.toLowerCase();
			setHTML(text + sep);
			setStyleName("chooser-item");
		}

		@Override
		public boolean filter(String filterText) {
			boolean b = filter.test(item, filterableText, filterText)
					&& (!selectedItems.contains(item)
							|| showSelectedItemsInSearch);
			setVisible(b);
			if (b && !ownerLabel.isVisible()) {
				ownerLabel.setVisible(true);
			}
			return b;
		}

		@Override
		public T getItem() {
			return item;
		}
	}

	public class SelectWithSearchItemX extends SpanPanel
			implements VisualFilterable, HasItem<T>, HasClickHandlers {
		private String filterableText;

		private final T item;

		private final Label ownerLabel;

		private Link hl;

		public SelectWithSearchItemX(T item, boolean asHTML, int charWidth,
				boolean withLfs, Label ownerLabel, String sep) {
			String text = (String) renderer.apply(item);
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
			ClientUtils.setImageDescendantTitle(hl, "delete selected item");
			hl.setUserObject(item);
			add(label);
			add(hl);
			setStyleName("selectx");
		}

		@Override
		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return hl.addClickHandler(handler);
		}

		@Override
		public boolean filter(String filterText) {
			boolean b = filterableText.contains(filterText)
					&& !selectedItems.contains(item);
			setVisible(b);
			if (b && !ownerLabel.isVisible()) {
				ownerLabel.setVisible(true);
			}
			return b;
		}

		@Override
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
}