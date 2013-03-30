package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Rect;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel.PositionCallback;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class RelativePopupPositioning {
	public static final String RENDER_CONTEXT_BOUNDING_PARENT = "RENDER_CONTEXT_BOUNDING_PARENT";

	public static final String CONTEXT_KEEP_RELATIVE_PARENT_CLIP = "CONTEXT_KEEP_RELATIVE_PARENT_CLIP";

	private static int INVALID = -99999;

	public static final RelativePopupAxis BOTTOM_RTL = new RelativePopupAxis(
			new AxisCoordinate[] { AxisCoordinate.H_RIGHT,
					AxisCoordinate.H_CENTER, AxisCoordinate.H_LEFT },
			AxisCoordinate.V_BOTTOM);

	public static final RelativePopupAxis BOTTOM_LTR = new RelativePopupAxis(
			new AxisCoordinate[] { AxisCoordinate.H_LEFT,
					AxisCoordinate.H_CENTER, AxisCoordinate.H_RIGHT },
			AxisCoordinate.V_BOTTOM);

	public static final RelativePopupAxis LEFT_TTB = new RelativePopupAxis(
			new AxisCoordinate[] { AxisCoordinate.V_TOP,
					AxisCoordinate.V_CENTER, AxisCoordinate.V_BOTTOM },
			AxisCoordinate.H_LEFT);

	public static RelativePopupPositioningParams forAxes(
			RelativePopupAxis[] axes) {
		RelativePopupPositioningParams params = new RelativePopupPositioningParams();
		params.axes = axes;
		return params;
	}

	public static RelativePopupPositioningParams forNativeEvent(
			OtherPositioningStrategy positioningStrategy,
			NativeEvent nativeEvent) {
		RelativePopupPositioningParams params = new RelativePopupPositioningParams();
		params.positioningStrategy = positioningStrategy;
		params.shiftToEventXY = true;
		params.nativeEvent = nativeEvent;
		return params;
	}

	public static Widget getCurrentBoundingParent() {
		return RenderContext.get().get(RENDER_CONTEXT_BOUNDING_PARENT);
	}

	public static void setCurrentBoundingParent(Widget boundingParent) {
		RenderContext.get().set(RENDER_CONTEXT_BOUNDING_PARENT, boundingParent);
	}

	public static RelativePopupPanel showPopup(Widget relativeToWidget,
			Widget widgetToShow, Widget boundingWidget, RelativePopupAxis axis) {
		return showPopup(relativeToWidget, widgetToShow, boundingWidget, axis,
				null, null);
	}

	public static RelativePopupPanel showPopup(Widget relativeToWidget,
			Widget widgetToShow, Widget boundingWidget, RelativePopupAxis axis,
			String panelStyleName, Widget relativeContainer) {
		RelativePopupPanel rpp = new RelativePopupPanel(true);
		rpp.setAnimationEnabled(true);
		if (panelStyleName != null) {
			rpp.addStyleName(panelStyleName);
		}
		return showPopup(relativeToWidget, widgetToShow, boundingWidget,
				new RelativePopupAxis[] { axis }, relativeContainer, rpp, 0, 0);
	}

	public static RelativePopupPanel showPopup(final Widget relativeToWidget,
			final Widget widgetToShow, final Widget boundingWidget,
			final RelativePopupAxis[] axes, Widget relativeContainer,
			final RelativePopupPanel rpp, final int shiftX, final int shiftY) {
		relativeContainer = relativeContainer != null ? relativeContainer
				: WidgetUtils.getPositioningParent(relativeToWidget);
		return showPopup(relativeToWidget.getElement(), widgetToShow,
				boundingWidget, forAxes(axes), relativeContainer, rpp, shiftX,
				shiftY);
	}

	public static RelativePopupPanel showPopup(final Widget elementContainer,
			final Widget widgetToShow, final Widget boundingWidget,
			RelativePopupPositioningParams positioningParams,
			Widget relativeContainer, final RelativePopupPanel rpp) {
		relativeContainer = relativeContainer != null ? relativeContainer
				: WidgetUtils.getPositioningParent(elementContainer);
		int shiftX = 0, shiftY = 0;
		if (positioningParams.shiftToEventXY
				&& positioningParams.nativeEvent != null) {
			NativeEvent nativeEvent = Event.as(Event.getCurrentEvent());
			shiftX = getRelativeX(elementContainer.getElement(),
					positioningParams.nativeEvent);
			shiftY = getRelativeY(elementContainer.getElement(),
					positioningParams.nativeEvent);
		}
		return showPopup(elementContainer.getElement(), widgetToShow,
				boundingWidget, positioningParams, relativeContainer, rpp,
				shiftX, shiftY);
	}

	public static int getRelativeX(Element target, NativeEvent e) {
		return e.getClientX() - target.getAbsoluteLeft()
				+ target.getScrollLeft()
				+ target.getOwnerDocument().getScrollLeft();
	}

	/**
	 * Gets the mouse y-position relative to a given element.
	 * 
	 * @param target
	 *            the element whose coordinate system is to be used
	 * @return the relative y-position
	 */
	public static int getRelativeY(Element target, NativeEvent e) {
		return e.getClientY() - target.getAbsoluteTop() + target.getScrollTop()
				+ target.getOwnerDocument().getScrollTop();
	}

	public static void ensurePopupWithin(RelativePopupPanel rpp,
			Widget boundingWidget) {
		int rw = rpp.getOffsetWidth();
		int rh = rpp.getOffsetHeight();
		int rl = rpp.getAbsoluteLeft();
		int x = boundingWidget.getAbsoluteLeft();
		int y = boundingWidget.getAbsoluteTop();
		int bwW = boundingWidget.getOffsetWidth();
		int bwH = boundingWidget.getOffsetHeight();
		if (rl + rw > x + bwW) {
			String pxl = rpp.getElement().getStyle().getLeft();
			if (pxl.endsWith("px")) {
				rpp.getElement()
						.getStyle()
						.setLeft(
								Integer.parseInt(pxl.replace("px", ""))
										- (rl + rw - x - bwW), Unit.PX);
			}
		}
	}

	private static RelativePopupPanel showPopup(
			final Element relativeToElement0, final Widget widgetToShow,
			final Widget boundingWidget,
			final RelativePopupPositioningParams positioningParams,
			Widget relativeContainer, final RelativePopupPanel rpp,
			final int shiftX, final int shiftY) {
		final Widget positioningWidget = relativeContainer;
		final Element relativeToElement = WidgetUtils
				.getElementForAroundPositioning(relativeToElement0);
		if (!LooseContext.getContext().getBoolean(
				CONTEXT_KEEP_RELATIVE_PARENT_CLIP)) {
			if (!BrowserMod.isIEpre9()) {
				Style style = positioningWidget.getElement().getStyle();
				style.clearProperty("clip");
			}// ie<9 doesn't like zat
		}
		if (widgetToShow != null) {
			rpp.setWidget(widgetToShow);
		}
		ComplexPanel cp = WidgetUtils.complexChildOrSelf(positioningWidget);
		rpp.setPositioningContainer(cp);
		rpp.setPopupPositionAndShow(new RelativePositioningCallback(rpp,
				relativeToElement, shiftX, shiftY, boundingWidget,
				positioningParams, positioningWidget));
		return rpp;
	}

	static class RelativePositioningCallback implements PositionCallback {
		private RelativePopupPanel rpp;

		private Element relativeToElement;

		private int shiftX;

		private int shiftY;

		private Widget boundingWidget;

		private RelativePopupPositioningParams positioningParams;

		private Widget positioningWidget;

		public RelativePositioningCallback(RelativePopupPanel rpp,
				Element relativeToElement, int shiftX, int shiftY,
				Widget boundingWidget,
				RelativePopupPositioningParams positioningParams,
				Widget positioningWidget) {
			this.rpp = rpp;
			this.relativeToElement = relativeToElement;
			this.shiftX = shiftX;
			this.shiftY = shiftY;
			this.boundingWidget = boundingWidget;
			this.positioningParams = positioningParams;
			this.positioningWidget = positioningWidget;
		}

		@Override
		public void setPosition(int offsetWidth, int offsetHeight) {
			int x = relativeToElement.getAbsoluteLeft();
			int y = relativeToElement.getAbsoluteTop();
			int relW = relativeToElement.getOffsetWidth();
			int relH = relativeToElement.getOffsetHeight();
			x += shiftX;
			y += shiftY;
			int rw = rpp.getOffsetWidth();
			int rh = rpp.getOffsetHeight();
			// work in coordinate space of bounding widget
			x -= boundingWidget.getAbsoluteLeft();
			y -= boundingWidget.getAbsoluteTop();
			int fixedAxisOffset = INVALID;
			int freeAxisOffset = INVALID;
			AxisCoordinate fixedAxis = null;
			AxisCoordinate freeAxis = null;
			int bw = boundingWidget.getOffsetWidth();
			int bh = boundingWidget.getOffsetHeight();
			final int bx = boundingWidget.getAbsoluteLeft();
			final int by = boundingWidget.getAbsoluteTop();
			int axisIndex = 0;
			RelativePopupAxis[] axes = positioningParams.axes;
			if (axes == null) {
				// absolute, internal to bounding widget
				switch (positioningParams.positioningStrategy) {
				case BELOW_WITH_PREFERRED_LEFT:
					x += positioningParams.shiftX;
					x -= positioningParams.preferredLeft;
					if (x < 0) {
						x = 0;
					}
					if (x + rw > bw) {
						x = bw - rw;
					}
					y += positioningParams.shiftY;
					break;
				case RIGHT_OR_LEFT_WITH_PREFERRED_TOP:
					x += 2;
					int clientY = positioningParams.nativeEvent.getClientY();
					int clientHeight = Window.getClientHeight();
					int oy = 0;
					if (clientY > positioningParams.preferredTop) {
						oy = Math.min(rh, clientY);
						oy = Math.max(0, oy
								- positioningParams.preferredFromBottom);
					} else {
						oy = Math.min(positioningParams.preferredTop, clientY);
					}
					y -= oy;
					if (rw + x > bw) {
						x -= (rw + 4);
					} else {
					}
					break;
				}
				x += boundingWidget.getAbsoluteLeft();
				y += boundingWidget.getAbsoluteTop();
			} else {
				for (RelativePopupAxis axis : axes) {
					fixedAxis = axis.fixedAxis;
					fixedAxisOffset = fixedAxis.fit(x, y, bw, bh, relW, relH,
							offsetWidth, offsetHeight, bx, by, null, false,
							false);
					if (fixedAxisOffset == INVALID) {
						continue;
					}
					AxisCoordinate last = null;
					for (int i = 0; i < 2; i++) {
						freeAxis = axis.freeAxis[i];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, bx, by, last, true,
								false);
						if (freeAxisOffset != INVALID) {
							break;
						}
						last = freeAxis;
					}
				}
				// always fall back to the first axis
				if (fixedAxisOffset == INVALID || freeAxisOffset == INVALID) {
					RelativePopupAxis axis = axes[0];
					fixedAxis = axis.fixedAxis;
					fixedAxisOffset = fixedAxis.fit(x, y, bw, bh, relW, relH,
							offsetWidth, offsetHeight, bx, by, null, false,
							true);
					AxisCoordinate last = null;
					for (int i = 0; i < 2; i++) {
						freeAxis = axis.freeAxis[i];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, bx, by, last, true,
								false);
						if (freeAxisOffset != INVALID) {
							break;
						}
						last = freeAxis;
					}
					if (freeAxisOffset == INVALID) {
						freeAxis = axis.freeAxis[0];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, bx, by, null, true,
								true);
					}
				}
				x = fixedAxis.isVertical() ? freeAxisOffset : fixedAxisOffset;
				y = freeAxis.isVertical() ? freeAxisOffset : fixedAxisOffset;
				// adjust to relative container
				x += boundingWidget.getAbsoluteLeft();
				y += boundingWidget.getAbsoluteTop();
				x -= positioningWidget.getAbsoluteLeft();
				y -= positioningWidget.getAbsoluteTop();
				//since we're relative, cancel the scroll
//				x+=WidgetUtils.getScrollLeft(positioningWidget.getElement());
//				y+=WidgetUtils.getScrollTop(positioningWidget.getElement());
			}
			rpp.setPopupPosition(x, y);
		}
	}

	public enum OtherPositioningStrategy {
		BELOW_WITH_PREFERRED_LEFT, RIGHT_OR_LEFT_WITH_PREFERRED_TOP
	}

	public static class RelativePopupAxis {
		public final AxisCoordinate[] freeAxis;

		public final AxisCoordinate fixedAxis;

		private RelativePopupAxis(AxisCoordinate[] freeAxis,
				AxisCoordinate fixedAxis) {
			this.freeAxis = freeAxis;
			this.fixedAxis = fixedAxis;
		}
	}

	public static class RelativePopupPositioningParams {
		public NativeEvent nativeEvent;

		public int preferredFromBottom;

		public RelativePopupAxis[] axes;

		public OtherPositioningStrategy positioningStrategy;

		public boolean shiftToEventXY = false;

		public int shiftY;

		public int preferredLeft;

		public int preferredTop;

		public int shiftX;
	}

	enum AxisCoordinate {
		H_LEFT {
			@Override
			public AxisType axisType() {
				return AxisType.NEG;
			}

			@Override
			public boolean isVertical() {
				return false;
			}
		},
		H_CENTER {
			@Override
			public AxisType axisType() {
				return AxisType.CENTER;
			}

			@Override
			public boolean isVertical() {
				return false;
			}
		},
		H_RIGHT {
			@Override
			public AxisType axisType() {
				return AxisType.POS;
			}

			@Override
			public boolean isVertical() {
				return false;
			}
		},
		V_TOP {
			@Override
			public AxisType axisType() {
				return AxisType.NEG;
			}

			@Override
			public boolean isVertical() {
				return true;
			}
		},
		V_CENTER {
			@Override
			public AxisType axisType() {
				return AxisType.CENTER;
			}

			@Override
			public boolean isVertical() {
				return true;
			}
		},
		V_BOTTOM {
			@Override
			public AxisType axisType() {
				return AxisType.POS;
			}

			@Override
			public boolean isVertical() {
				return true;
			}
		};
		abstract AxisType axisType();

		/**
		 * The logic's doesn't quite seem to map here - but it does really. If
		 * "wrapping relative to" - e.g. free axis of BOTTOM_LTR then left is
		 * popup-left aligned to relative-to-widget-left right same. If not
		 * (fixed axis), in example bottom == popup-top aligned to
		 * relative-bottom
		 * 
		 */
		int fit(int relX, int relY, int bw, int bh, int relW, int relH,
				int ppW, int ppH, int bx, int by, AxisCoordinate favour,
				boolean wrappingRelativeTo, boolean force) {
			int relC = relX;
			int bDim = bw;
			int relDim = relW;
			int ppDim = ppW;
			int boDim = bx;
			int rDim = RootPanel.get().getOffsetWidth();
			if (isVertical()) {
				relC = relY;
				bDim = bh;
				relDim = relH;
				ppDim = ppH;
				boDim = by;
				rDim = RootPanel.get().getOffsetHeight();
			}
			int result = 0;
			switch (axisType()) {
			case NEG:
				result = wrappingRelativeTo ? relC : relC - ppDim;
				break;
			case POS:
				result = wrappingRelativeTo ? relC + relDim - ppDim : relC
						+ relDim;
				break;
			case CENTER: // wrappingRelativeTo == true
				if (favour.axisType() != null) {
					switch (favour.axisType()) {
					case NEG:
						result = bDim - ppDim;
						result = Math.max(result, -boDim);
						// make as close to "left-align"
						// as poss
						break;
					case POS:
						result = Math.min(0, rDim - ppDim - bDim);
						// as close to 'right-align' as poss
						break;
					default:
						break;
					}
				} else {
					result = INVALID;
				}
			}
			if (result < 0 || result + ppDim > bDim) {
				result = INVALID;
			}
			if (force && result == INVALID) {
				result = relC + (wrappingRelativeTo ? 0 : relDim);
			}
			return result;
		}

		abstract boolean isVertical();
	}

	enum AxisType {
		NEG, CENTER, POS
	}

	public static void scrollIntoViewWhileKeepingRect(Rect bounds,
			DecoratedRelativePopupPanel popup) {
		// TODO Auto-generated method stub
	}
}
