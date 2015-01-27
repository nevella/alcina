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
package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.widget.HasComplexPanel;
import cc.alcina.framework.gwt.client.widget.TreeNodeWalker;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlers;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo.LayoutInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */
public class WidgetUtils {
	public static void populateListFromEnum(ListBox box, Object[] objs) {
		for (Object obj : objs) {
			String k = obj.toString();
			String friendly = k.toLowerCase().replace('_', ' ');
			box.addItem(friendly, k);
		}
	}

	public static void toggleStyleName(Widget w, String styleName) {
		String current = w.getStyleName();
		if (current.contains(styleName)) {
			w.removeStyleName(styleName);
		} else {
			w.addStyleName(styleName);
		}
	}

	public static void addOrRemoveStyleName(Widget w, String styleName,
			boolean add) {
		if (!add) {
			w.removeStyleName(styleName);
		} else {
			w.addStyleName(styleName);
		}
	}

	// TODO - check all calls here either the cp implements haschildhandlers, or
	// explain why t'hell not...(doesn't add handlers to the child widgets would
	// be a good reason)
	public static void clearChildren(ComplexPanel cp) {
		for (int i = cp.getWidgetCount() - 1; i >= 0; i--) {
			Widget widget = cp.getWidget(i);
			cp.remove(i);
		}
		if (cp instanceof HasChildHandlers) {
			HasChildHandlers hch = (HasChildHandlers) cp;
			hch.detachHandlers();
		}
	}

	public static void setColumnVisibility(HTMLTable table, int column,
			boolean visible) {
		int rows = table.getRowCount();
		for (int row = 0; row < rows; row++) {
			UIObject.setVisible(table.getCellFormatter()
					.getElement(row, column), visible);
		}
	}

	public static Widget getPositioningParent(Widget widget) {
		while (widget.getParent() != null) {
			String pos = getComputedStyle(widget.getElement(), "position");
			if (pos != null
					&& (pos.equals("relative") || pos.equals("absolute"))) {
				return widget;
			}
			widget = widget.getParent();
		}
		return widget;// root panel
	}

	public static ComplexPanel complexChildOrSelf(Widget w) {
		if (w instanceof ComplexPanel) {
			return (ComplexPanel) w;
		}
		if (w instanceof HasComplexPanel) {
			return ((HasComplexPanel) w).getComplexPanel();
		}
		if (w instanceof SimplePanel) {
			return complexChildOrSelf(((SimplePanel) w).getWidget());
		}
		return RootPanel.get();
	}

	public static native String getComputedStyle(Element elt,
			String attributeName)/*-{
	if (elt.currentStyle) {
	    return elt.currentStyle[attributeName];
	}
	if ($wnd.getComputedStyle) {
	    return $wnd.getComputedStyle(elt, null)[attributeName];
	}
	}-*/;

	public static void clearChildren(TabPanel tp) {
		for (int i = tp.getWidgetCount() - 1; i >= 0; i--) {
			tp.remove(i);
		}
	}

	public static List<Widget> allChildren(HasWidgets p) {
		List<Widget> widgets = new ArrayList<Widget>();
		if (p instanceof Widget) {
			widgets.add((Widget) p);
		}
		Iterator<Widget> iterator = p.iterator();
		while (iterator.hasNext()) {
			Widget w = iterator.next();
			if (w instanceof HasWidgets) {
				widgets.addAll(allChildren((HasWidgets) w));
			} else {
				widgets.add(w);
			}
		}
		return widgets;
	}

	public static int getBestOffsetHeight(Element e) {
		return getBestOffsetHeight(e, false);
	}

	private static int getBestOffsetHeight(Element e, boolean parentPass) {
		int h = e.getPropertyInt("offsetHeight");
		if (h != 0 || e.getParentElement() == null) {
			return h;
		}
		if (e.getFirstChildElement() == null && !parentPass) {
			return getBestOffsetHeight(e, true);
		}
		return getBestOffsetHeight(
				parentPass ? e.getParentElement() : e.getFirstChildElement(),
				parentPass);
	}

	public static int getBestOffsetWidth(Element e) {
		return getBestOffsetWidth(e, false);
	}

	private static int getBestOffsetWidth(Element e, boolean parentPass) {
		int h = e.getPropertyInt("offsetWidth");
		if (h != 0 || e.getParentElement() == null) {
			return h;
		}
		if (e.getFirstChildElement() == null && !parentPass) {
			return getBestOffsetWidth(e, true);
		}
		return getBestOffsetWidth(
				parentPass ? e.getParentElement() : e.getFirstChildElement(),
				parentPass);
	}

	private static boolean debug = false;

	public static void resizeUsingInfo(int containerHeight, int containerWidth,
			Iterator<Widget> widgets, int parentAdjustHeight,
			int parentAdjustWidth) {
		while (widgets.hasNext()) {
			Widget widget = widgets.next();
			if (widget == null || !widget.isVisible()) {
				continue;
			}
			int availableHeight = containerHeight;
			int availableWidth = containerWidth;
			if (widget instanceof HasLayoutInfo) {
				if (debug) {
					GWT.log(CommonUtils.formatJ("%s: ",
							CommonUtils.simpleClassName(widget.getClass())),
							null);
				}
				LayoutInfo info = ((HasLayoutInfo) widget).getLayoutInfo();
				info.beforeLayout();
				if (info.to100percentOfAvailableHeight()
						|| info.to100percentOfAvailableWidth()) {
					int usedHeight = 0;
					int usedWidth = 0;
					Widget parent = widget.getParent();
					Iterator<Widget> childIterator = null;
					availableHeight = info.useBestOffsetForParentHeight() ? getBestOffsetHeight(
							parent.getElement(), true) : containerHeight;
					availableHeight = Math
							.min(availableHeight, containerHeight);
					availableWidth = info.useBestOffsetForParentWidth() ? getBestOffsetWidth(
							parent.getElement(), true) : containerWidth;
					availableWidth = Math.min(availableWidth, containerWidth);
					if (parent instanceof HasLayoutInfo) {
						childIterator = ((HasLayoutInfo) parent)
								.getLayoutInfo().getLayoutWidgets();
					} else if (parent instanceof HasWidgets) {
						childIterator = ((HasWidgets) parent).iterator();
					}
					boolean ignoreChildrenForHeight = info
							.to100percentOfAvailableHeight()
							&& (isDirectionalLayoutPanel(parent, true) || info
									.ignoreSiblingsForHeight());
					boolean ignoreChildrenForWidth = info
							.to100percentOfAvailableWidth()
							&& (isDirectionalLayoutPanel(parent, false) || info
									.ignoreSiblingsForWidth());
					if (childIterator != null) {
						while (childIterator.hasNext()) {
							Widget cw = childIterator.next();
							if (cw != widget && cw.isVisible()) {
								if (!ignoreChildrenForHeight) {
									usedHeight += getBestOffsetHeight(cw
											.getElement());
								}
								if (!ignoreChildrenForWidth) {
									usedWidth += getBestOffsetWidth(cw
											.getElement());
								}
							}
						}
					}
					if (info.to100percentOfAvailableHeight()) {
						availableHeight = availableHeight - usedHeight
								- parentAdjustHeight - info.getAdjustHeight();
						if (debug) {
							GWT.log(CommonUtils.formatJ("%s: %s - comp %s",
									CommonUtils.simpleClassName(widget
											.getClass()), availableHeight,
									containerHeight), null);
						}
						if (availableHeight >= 0) {
							widget.setHeight((availableHeight) + "px");
						}
					}
					if (info.to100percentOfAvailableWidth()) {
						availableWidth = availableWidth - usedWidth
								- parentAdjustWidth - info.getAdjustWidth();
						if (availableWidth >= 0) {
							widget.setWidth((availableWidth) + "px");
						}
					}
				}
				Iterator<Widget> toResize = info.getWidgetsToResize();
				while (toResize.hasNext()) {
					toResize.next().setHeight(containerHeight + "px");
				}
				resizeUsingInfo(availableHeight, availableWidth,
						info.getLayoutWidgets(), info.getClientAdjustHeight(),
						info.getClientAdjustWidth());
				info.afterLayout();
			}// haslayoutinfo
			else if (widget instanceof HasWidgets) {
				resizeUsingInfo(availableHeight, availableWidth,
						((HasWidgets) widget).iterator(), 0, 0);
			}
		}// while
	}

	private static boolean isDirectionalLayoutPanel(Widget panel,
			boolean horizontal) {
		if (panel instanceof DockLayoutPanel) {
			DockLayoutPanel dlp = (DockLayoutPanel) panel;
			Iterator<Widget> itr = dlp.iterator();
			for (Widget widget : dlp) {
				Direction dir = dlp.getWidgetDirection(widget);
				if (horizontal
						&& (dir == Direction.NORTH || dir == Direction.SOUTH)) {
					return false;
				}
				if (!horizontal
						&& (dir == Direction.WEST || dir == Direction.EAST)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static void resizeUsingInfo(Widget w) {
		resizeUsingInfo(getBestOffsetHeight(w.getElement()),
				getBestOffsetWidth(w.getElement()), w);
	}

	public static void resizeUsingInfo(int availableHeight, int availableWidth,
			Widget w) {
		resizeUsingInfo(availableHeight, availableWidth,
				Arrays.asList(new Widget[] { w }).iterator(), 0, 0);
	}

	public static void replace(Widget current, Widget newWidget) {
		if (current.getParent() instanceof SimplePanel) {
			((SimplePanel) current.getParent()).setWidget(newWidget);
			return;
		}
		ComplexPanel cp = (ComplexPanel) current.getParent();
		int index = cp.getWidgetIndex(current);
		cp.remove(index);
		if (cp instanceof FlowPanel) {
			FlowPanel fp = (FlowPanel) cp;
			fp.insert(newWidget, index);
		}
	}

	public static void setOpacity(Widget w, int opacityPercent) {
		com.google.gwt.user.client.Element e = w.getElement();
		String opacity = opacityPercent == 100 ? "1.0" : "0."
				+ CommonUtils.padTwo(opacityPercent);
		DOM.setStyleAttribute(e, "opacity", opacity);
		DOM.setStyleAttribute(e, "mozOpacity", opacity);
		DOM.setStyleAttribute(e, "filter", " alpha(opacity=" + opacityPercent
				+ ")");
	}

	public static void scrollIntoView(Element e) {
		scrollIntoView(e, 0);
	}

	public static void scrollBodyTo(int y) {
		BodyElement body = Document.get().getBody();
		body.setPropertyInt("scrollTop", y);
		Element documentElement = Document.get().getDocumentElement();
		documentElement.setPropertyInt("scrollTop", y);
		Window.scrollTo(0, y);
	}

	private static List<Widget> hiddenWidgets;

	private static List<SplitLayoutPanel> morphedWidgets;

	private static List<ElementLayout> elementLayouts;

	private static Text tempPositioningText;

	public static void releaseTempPositioningText() {
		if (tempPositioningText != null) {
			tempPositioningText.removeFromParent();
			tempPositioningText = null;
		}
	}

	public static boolean hasTempPositioningText() {
		return tempPositioningText != null;
	}

	public static void maximiseWidget(Widget widget) {
		restoreFromMaximise();
		hiddenWidgets = new ArrayList<Widget>();
		morphedWidgets = new ArrayList<SplitLayoutPanel>();
		elementLayouts = new ArrayList<ElementLayout>();
		Element e = widget.getElement();
		while (widget.getParent() != null) {
			Widget parent = widget.getParent();
			if (parent instanceof SplitLayoutPanel) {
				morphSplitPanel((SplitLayoutPanel) parent, widget, false);
			} else if (parent instanceof HasWidgets
					&& !(parent instanceof TabPanel)) {
				HasWidgets hw = (HasWidgets) parent;
				for (Iterator<Widget> itr = hw.iterator(); itr.hasNext();) {
					Widget w = itr.next();
					if (w != widget && w.isVisible()) {
						hiddenWidgets.add(w);
						w.setVisible(false);
					}
				}
			}
			widget = widget.getParent();
		}
		while (e.getParentElement() != RootPanel.get().getElement()) {
			ElementLayout layout = new ElementLayout(e);
			elementLayouts.add(layout);
			layout.maximise();
			e = e.getParentElement();
		}
	}

	public static Element getElementForAroundPositioning(Element from) {
		boolean hidden = isZeroOffsetDims(from);
		if (!isZeroOffsetDims(from)) {
			return from;
		}
		ClientNodeIterator itr = new ClientNodeIterator(from,
				ClientNodeIterator.SHOW_ELEMENT);
		Element elt = null;
		while ((elt = (Element) itr.nextNode()) != null) {
			if (!isZeroOffsetDims(elt)) {
				return elt;
			}
		}
		while (elt != null) {
			if (!isZeroOffsetDims(elt)) {
				return elt;
			}
			elt = elt.getParentElement();
		}
		return null;
	}

	public static Element getElementForPositioning0(Element from) {
		assert tempPositioningText == null;
		if (!isVisibleAncestorChain(from)) {
			return null;
		}
		boolean hidden = isZeroOffsetDims(from);
		int kidCount = from.getChildCount();
		if (kidCount != 0 && !hidden) {
			return from;
		}
		Node parent = from.getParentNode();
		if (parent != null && parent.getFirstChild() == from
				&& parent.getNodeType() == Node.ELEMENT_NODE
				&& !isZeroOffsetDims((Element) parent)) {
			return (Element) parent;
		}
		ClientNodeIterator itr = new ClientNodeIterator(from,
				ClientNodeIterator.SHOW_ALL);
		Element fromContainingBlock = DomUtils.getContainingBlock(from);
		Node node = from;
		int insertTextIfOffsetMoreThanXChars = 100;
		while ((node = node.getPreviousSibling()) != null) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				insertTextIfOffsetMoreThanXChars -= TextUtils.normaliseAndTrim(
						node.getNodeValue()).length();
				if (insertTextIfOffsetMoreThanXChars < 0) {
					// this causes a relayout - so we try and avoid. most of the
					// time, positioning elements will contain text (or be from
					// a friendly browser), or be at the start of a block elt)
					tempPositioningText = Document.get().createTextNode("---");
					from.appendChild(tempPositioningText);
					return from;
				}
			}
		}
		while ((node = itr.nextNode()) != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (!isZeroOffsetDims(node.getParentElement())
						&& node.getNodeName().equalsIgnoreCase("img")) {
					return (Element) node;
				}
			} else {
				// text
				if (!isZeroOffsetDims(node.getParentElement())
				// we don't want the combined ancestor of everyone...
						&& (!node.getParentElement().isOrHasChild(from) ||
						// but we do want <p><a><b>*some-text*</b></p>
						DomUtils.getContainingBlock(node) == fromContainingBlock)) {
					return node.getParentElement();
				}
			}
		}
		return from.getParentElement();
	}

	public static native Element getFocussedDocumentElement()/*-{
	if ($doc.activeElement) {
	    var tagName = $doc.activeElement.tagName.toLowerCase();
	    return tagName != "body" && tagName != "html" ? $doc.activeElement
		    : null;
	}
	return null;
	}-*/;

	public static native void clearFocussedDocumentElement()/*-{
	if ($doc.activeElement) {
	    var tagName = $doc.activeElement.tagName.toLowerCase();
	    if (tagName != "body" && tagName != "html") {
		$doc.activeElement.blur();
	    }
	}
	}-*/;

	// those values might be needed for non-webkit
	@SuppressWarnings("unused")
	private static class ElementLayout {
		private String position;

		private String height;

		private String width;

		// ff3.5, at least
		private String overflow;

		private final Element element;

		public ElementLayout(Element element) {
			this.element = element;
		}

		void maximise() {
			position = element.getStyle().getProperty("position");
			// width = element.getStyle().getProperty("width");
			// height = element.getStyle().getProperty("height");
			overflow = element.getStyle().getProperty("overflow");
			element.getStyle().setProperty("position", "");
			// element.getStyle().setProperty("width", "");
			// element.getStyle().setProperty("height", "");
			element.getStyle().setProperty("overflow", "");
		}

		void restore() {
			element.getStyle().setProperty("position", position);
			// element.getStyle().setProperty("width", width);
			// element.getStyle().setProperty("height", height);
			element.getStyle().setProperty("overflow", overflow);
		}
	}

	private static void morphSplitPanel(SplitLayoutPanel splitPanel,
			Widget keepChild, boolean restore) {
		final String zeroSize = "0px";
		boolean hsp = isDirectionalLayoutPanel(splitPanel, true);
		for (int index = 0; index < splitPanel.getWidgetCount(); index++) {
			Widget w = splitPanel.getWidget(index);
			if (CommonUtils.simpleClassName(w.getClass()).contains("Splitter")) {
				w.setVisible(restore);
			} else {
				Element container = splitPanel.getWidgetContainerElement(w);
				container.getStyle().setDisplay(
						restore || keepChild == w ? Display.BLOCK
								: Display.NONE);
			}
		}
		if (!restore) {
			morphedWidgets.add(splitPanel);
		}
		// splitPanel.getWidgetCount()
		// com.google.gwt.user.client.Element root, splitter, container0,
		// container1, contained0, contained1, keepDisplaying;
		// root = (com.google.gwt.user.client.Element) splitPanel.getElement()
		// ;
		// String str = root.getString();
		// NodeList<Node> childNodes = root.getChildNodes();
		// container0 = (com.google.gwt.user.client.Element)
		// childNodes.getItem(0);
		// splitter = (com.google.gwt.user.client.Element)
		// childNodes.getItem(1);
		// container1 = (com.google.gwt.user.client.Element)
		// childNodes.getItem(2);
		// contained0 = (com.google.gwt.user.client.Element) container0
		// .getChildNodes().getItem(0);
		// contained1 = (com.google.gwt.user.client.Element) container1
		// .getChildNodes().getItem(0);
		// int splitPos = hsp ? container0.getOffsetWidth() : container0
		// .getOffsetHeight();
		// String display = restore ? "" : "none";
		// String position = restore ? "absolute" : "";
		// splitter.getStyle().setProperty("display", display);
		// container0.getStyle().setProperty("display", display);
		// container1.getStyle().setProperty("display", display);
		// container0.getStyle().setProperty("position", position);
		// container1.getStyle().setProperty("position", position);
		// if (!restore) {
		// keepDisplaying = contained0 == keepChild.getElement() ? container0
		// : container1;
		// keepDisplaying.getStyle().setProperty("display", "");
		// morphedWidgets.add(splitPanel);
		// root.setPropertyInt(SPLIT_PANEL_RESTORE_PROP, splitPos);
		// } else {
		// splitPos = root.getPropertyInt(SPLIT_PANEL_RESTORE_PROP);
		// splitPanel.setWidgetSize(splitPanel.getWidget(0), splitPos);
		// }
	}

	private static final String SPLIT_PANEL_RESTORE_PROP = "_split_panel_restore";

	public static void restoreFromMaximise() {
		if (hiddenWidgets == null) {
			return;
		}
		// reverse order for webkit/gecko and (??)
		for (int i = elementLayouts.size() - 1; i >= 0; i--) {
			ElementLayout layout = elementLayouts.get(i);
			layout.restore();
		}
		for (SplitLayoutPanel w : morphedWidgets) {
			morphSplitPanel(w, null, true);
		}
		for (Widget w : hiddenWidgets) {
			w.setVisible(true);
		}
		hiddenWidgets = null;
	}

	@SuppressWarnings("unchecked")
	public static <W extends Widget> W getParentWidget(Widget w,
			Class<W> widgetClass) {
		while (w != null) {
			if (w.getClass() == widgetClass) {
				return (W) w;
			}
			w = w.getParent();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Widget getParentWidgetSatisfyingCallback(Widget w,
			CollectionFilter<Object> callback) {
		while (w != null) {
			if (callback.allow(w)) {
				return w;
			}
			w = w.getParent();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Widget> T getParentWidgetSatisfyingTypedCallback(
			Widget w, CollectionFilter<Widget> callback) {
		while (w != null) {
			if (callback.allow(w)) {
				return (T) w;
			}
			w = w.getParent();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <W extends Widget> W getParentWidget(Widget w,
			String widgetClassName) {
		while (w != null) {
			if (CommonUtils.simpleClassName(w.getClass()).equals(
					widgetClassName)) {
				return (W) w;
			}
			w = w.getParent();
		}
		return null;
	}

	public static void ensureNodeDebugIds(TreeItem root, final String prefix) {
		TreeNodeWalker tnw = new TreeNodeWalker();
		tnw.walk(root, new Callback<TreeItem>() {
			public void apply(TreeItem target) {
				target.ensureDebugId(prefix);
			}
		});
	}

	public static void setCssVisibility(Widget widget, boolean visible) {
		widget.getElement().getStyle()
				.setProperty("visibility", visible ? "visible" : "hidden");
	}

	public static void squelchCurrentEvent() {
		Event currentEvent = Event.getCurrentEvent();
		if (currentEvent != null) {
			currentEvent.stopPropagation();
			currentEvent.preventDefault();
		}
	}

	public static boolean isNewTabModifier() {
		Event event = Event.getCurrentEvent();
		return isNewTabModifier(event);
	}

	public static boolean isNewTabModifier(NativeEvent event) {
		return BrowserMod.getOperatingSystem().equals("Macintosh") ? event
				.getMetaKey() : event.getCtrlKey();
	}

	public static void copyTextToClipboard(String text) {
		FlowPanel fp = new FlowPanel();
		TextArea ta = new TextArea();
		ta.setSize("600px", "300px");
		ta.setText(text);
		fp.add(ta);
		PopupPanel pp = new PopupPanel();
		pp.add(fp);
		pp.setAnimationEnabled(false);
		pp.show();
		ta.setSelectionRange(0, text.length());
		try {
			copy();
		} catch (JavaScriptException e) {
			pp.hide();
			if (e.getMessage().contains("NS_ERROR_XPC_NOT_ENOUGH_ARGS")) {
				Registry.impl(ClientNotifications.class)
						.showMessage(
								new HTML(
										"<div class='info'>Sorry, clipboard operations"
												+ " are disabled by Mozilla/Firefox"
												+ " security settings. <br><br> Please see "
												+ "<a href='http://www.mozilla.org/editor/midasdemo/securityprefs.html'>"
												+ "http://www.mozilla.org/editor/midasdemo/securityprefs.html</a></div> "));
			} else {
				throw e;
			}
		}
		pp.hide();
	}

	private static native void copy() /*-{
	$doc.execCommand("Copy");
	}-*/;

	public static NativeEvent createZeroClick() {
		return Document.get().createClickEvent(0, 0, 0, 0, 0, false, false,
				false, false);
	}

	// TODO - actually Widget.fireEvent() would be much better here - duh...
	// nah - this is the right way
	public static void fireClickOnHandler(final HasClickHandlers source,
			final ClickHandler handler) {
		final HandlerRegistration handlerRegistration = source
				.addClickHandler(handler);
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				NativeEvent event = createZeroClick();
				DomEvent.fireNativeEvent(event, source);
				handlerRegistration.removeHandler();
			}
		});
	}

	public native static int getRelativeTopTo(Element elem, Element end) /*-{
	var top = 0;
	while (elem != end) {
	    top += elem.offsetTop;
	    elem = elem.offsetParent;
	}
	return top;
	}-*/;

	public static void scrollIntoView(Element e, int fromTop) {
		scrollIntoView(e, fromTop, false);
	}

	public static void scrollIntoView(Element e, int fromTop,
			boolean forceFromTop) {
		int y1 = Document.get().getBodyOffsetTop() + Window.getScrollTop();
		int y2 = y1 + Window.getClientHeight();
		Element parent = e.getParentElement();
		int absoluteTop = e.getAbsoluteTop();
		if (!forceFromTop && (absoluteTop >= y1 && absoluteTop < y2)) {
			return;
		}
		DOM.scrollIntoView((com.google.gwt.user.client.Element) e);
		y1 = Document.get().getBodyOffsetTop() + Window.getScrollTop();
		y2 = y1 + Window.getClientHeight();
		absoluteTop = e.getAbsoluteTop();
		if (absoluteTop < y1 || absoluteTop > y2 || fromTop != 0) {
			scrollBodyTo((Math.max(0, absoluteTop - fromTop)));
		}
	}

	public static void setOrRemoveStyleName(Widget widget, String styleName,
			boolean set) {
		if (set) {
			widget.addStyleName(styleName);
		} else {
			widget.removeStyleName(styleName);
		}
	}

	public static boolean isZeroOffsetDims(Element e) {
		return e.getOffsetHeight() == 0 || e.getOffsetWidth() == 0;
	}

	public static boolean isVisibleAncestorChain(Widget w) {
		return isVisibleAncestorChain(w.getElement());
	}

	public static boolean isVisibleAncestorChain(Element e) {
		while (e != null) {
			if (!UIObject.isVisible(e)) {
				return false;
			}
			if ("hidden".equals(e.getStyle().getVisibility())) {
				return false;
			}
			e = e.getParentElement();
		}
		return true;
	}

	public static FlowPanel wrapInDiv(Widget widget) {
		FlowPanel fp = new FlowPanel();
		fp.add(widget);
		return fp;
	}

	public static void disableTextBoxHelpers(Widget textBox) {
		com.google.gwt.user.client.Element elt = textBox.getElement();
		elt.setAttribute("autocapitalize", "off");
		elt.setAttribute("autocorrect", "off");
		elt.setAttribute("autocomplete", "off");
		elt.setAttribute("spellcheck", "false");
	}

	public static void copySize(Widget from, Widget to) {
		to.setSize(from.getOffsetWidth() + "px", from.getOffsetHeight() + "px");
	}

	public static void maybeClosePopupParent(ClickEvent clickEvent) {
		Widget w = (Widget) clickEvent.getSource();
		CollectionFilter<Object> callback = new CollectionFilter<Object>() {
			@Override
			public boolean allow(Object o) {
				return o instanceof RelativePopupPanel;
			}
		};
		RelativePopupPanel pp = (RelativePopupPanel) getParentWidgetSatisfyingCallback(
				w, callback);
		if (pp != null) {
			pp.hide();
		}
	}

	public static native int getScrollLeft(Element elem) /*-{
	var left = 0;
	var curr = elem;
	// This intentionally excludes body which has a null offsetParent.
	while (curr.offsetParent) {
	    left -= curr.scrollLeft;
	    curr = curr.parentNode;
	}

	return left;
	}-*/;

	public static native int getScrollTop(Element elem) /*-{
	var top = 0;
	var curr = elem;
	// This intentionally excludes body which has a null offsetParent.
	while (curr.offsetParent) {
	    top -= curr.scrollTop;
	    curr = curr.parentNode;
	}
	return top;
	}-*/;

	public static native Element getElementByNameOrId(Document doc, String name) /*-{

	var e = doc.getElementById(name);
	if (!e) {
	    e = doc.getElementsByName(name)
		    && doc.getElementsByName(name).length == 1 ? doc
		    .getElementsByName(name)[0] : null;
	}
	return e;
	}-*/;

	public static native String getComputedStyleProperty(Element elem,
			String strCssRule) /*-{
	if ($doc.defaultView && $doc.defaultView.getComputedStyle) {
	    strValue = $doc.defaultView.getComputedStyle(elem, "")
		    .getPropertyValue(strCssRule);
	} else if (oElm.currentStyle) {
	    strCssRule = strCssRule.replace(/\-(\w)/g, function(strMatch, p1) {
		return p1.toUpperCase();
	    });
	    strValue = oElm.currentStyle[strCssRule];
	}
	return strValue;
	}-*/;

	public static native int getOffsetHeightWithMargins(Element elem) /*-{
	if (elem.style.display == 'none') {
	    return 0;
	}
	var h = elem.offsetHeight;
	var marginTop = @cc.alcina.framework.gwt.client.util.WidgetUtils::getComputedStyle(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;)(elem,"margin");
	var marginBottom = @cc.alcina.framework.gwt.client.util.WidgetUtils::getComputedStyle(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;)(elem,"margin");
	if (marginTop.indexOf("px") != -1) {
	    h += parseInt(marginTop.substring(0, marginTop.length - 2));
	}
	if (marginBottom.indexOf("px") != -1) {
	    h += parseInt(marginBottom.substring(0, marginBottom.length - 2));
	}
	return h;
	}-*/;

	public static Element clickGetAnchorAncestor(ClickEvent clickEvent) {
		Event event = Event.as(clickEvent.getNativeEvent());
		// handle localisation spans
		Element target = null;
		if (!Element.is(event.getEventTarget())) {
			return null;
		}
		target = Element.as(event.getEventTarget());
		Element anchor = DomUtils.getAncestorWithTagName(target, "A");
		return anchor;
	}

	public static native NodeList getElementsForSelector(Element elt,
			String selector) /*-{
	if (!($doc.querySelector)) {
	    return null;
	}
	var from = (elt) ? elt : $doc;
	return from.querySelectorAll(selector);
	}-*/;

	public static native Element getElementForSelector(Element elt,
			String selector) /*-{
	if (!($doc.querySelector)) {
	    return null;
	}
	var from = (elt) ? elt : $doc;
	var splits = selector.split("::");
	for (var idx = 0; idx < splits.length; idx += 2) {
	    var selectorPart = splits[idx];
	    var textRegex = idx == splits.length - 1 ? null : splits[idx + 1];
	    if (textRegex == null) {
		return from.querySelector(selectorPart);
	    }
	    var nl = from.querySelectorAll(splits[idx]);
	    var found = false;
	    for (var i = 0; i < nl.length; i++) {
		var item = nl[i];
		if (item.innerHTML.indexOf(textRegex) != -1
			|| item.innerHTML.match(new RegExp(textRegex))) {
		    from = item;
		    found = true;
		    break;
		}
	    }
	    if (!found) {
		return null;
	    }
	}
	return from;
	}-*/;

	public static native void focus(Element elem) /*-{
	elem.focus();
	}-*/;

	public static final native void click(Element elt) /*-{
	elt.click();
	try {
	    elt.focus();
	} catch (e) {

	}
	}-*/;

	public static void hardCancelEvent(NativePreviewEvent event) {
		event.cancel();
		cancelPossibleIEShortcut();
	}

	private static native void cancelPossibleIEShortcut() /*-{
	try {
	    $wnd.event.keyCode = 0; // this is a hack to capture ctrl+f ctrl+p etc
	} catch (e) {

	}
	}-*/;

	public static int propertyPx(String propertyString) {
		if (propertyString.indexOf("px") == -1) {
			return 0;
		}
		return Integer.parseInt(propertyString.replace("px", ""));
	}
}
