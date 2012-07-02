package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.LooseContextProvider;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.objecttree.RenderContext;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.OtherPositioningStrategy;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupPositioningParams;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel.PositionCallback;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
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

	public static RelativePopupPositioningParams forMouse(
			OtherPositioningStrategy positioningStrategy, MouseEvent mouseEvent) {
		RelativePopupPositioningParams params = new RelativePopupPositioningParams();
		params.positioningStrategy = positioningStrategy;
		params.mouseEvent = mouseEvent;
		params.shiftToEventXY = true;
		return params;
	}

	public static Widget getCurrentBoundingParent() {
		return RenderContext.current().get(RENDER_CONTEXT_BOUNDING_PARENT);
	}

	public static void setCurrentBoundingParent(Widget boundingParent) {
		RenderContext.current().set(RENDER_CONTEXT_BOUNDING_PARENT,
				boundingParent);
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
				&& positioningParams.mouseEvent != null) {
			NativeEvent nativeEvent = Event.as(Event.getCurrentEvent());
			shiftX = positioningParams.mouseEvent.getRelativeX(elementContainer
					.getElement());
			shiftY = positioningParams.mouseEvent.getRelativeY(elementContainer
					.getElement());
		}
		return showPopup(elementContainer.getElement(), widgetToShow,
				boundingWidget, positioningParams, relativeContainer, rpp,
				shiftX, shiftY);
	}

	private static RelativePopupPanel showPopup(
			final Element relativeToElement, final Widget widgetToShow,
			final Widget boundingWidget,
			final RelativePopupPositioningParams positioningParams,
			Widget relativeContainer, final RelativePopupPanel rpp,
			final int shiftX, final int shiftY) {
		final Widget positioningWidget = relativeContainer;
		if (!LooseContextProvider.getContext().getBoolean(
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
		rpp.setPopupPositionAndShow(new PositionCallback() {
			public void setPosition(int offsetWidth, int offsetHeight) {
				int x = relativeToElement.getAbsoluteLeft();
				int y = relativeToElement.getAbsoluteTop();
				int relW = relativeToElement.getOffsetWidth();
				int relH = relativeToElement.getOffsetHeight();
				if (relH == 0) {
					Element parentElement = relativeToElement
							.getParentElement();
					relH = parentElement.getOffsetHeight();
					y = parentElement.getAbsoluteTop();
				}
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
				int axisIndex = 0;
				RelativePopupAxis[] axes = positioningParams.axes;
				if (axes == null) {
					// absolute, internal to bounding widget
					switch (positioningParams.positioningStrategy) {
					case BELOW_WITH_PREFERRED_LEFT:
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
						int clientY = positioningParams.mouseEvent.getClientY();
						int clientHeight = Window.getClientHeight();
						int oy = 0;
						if (clientY > positioningParams.preferredTop) {
							oy = Math.min(rh, clientY);
							oy = Math.max(0, oy
									- positioningParams.preferredFromBottom);
						} else {
							oy = Math.min(positioningParams.preferredTop,
									clientY);
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
						fixedAxisOffset = fixedAxis.fit(x, y, bw, bh, relW,
								relH, offsetWidth, offsetHeight, null, false,
								false);
						if (fixedAxisOffset == INVALID) {
							continue;
						}
						AxisCoordinate last = null;
						for (int i = 0; i < 2; i++) {
							freeAxis = axis.freeAxis[i];
							freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW,
									relH, offsetWidth, offsetHeight, last,
									true, false);
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
						fixedAxisOffset = fixedAxis.fit(x, y, bw, bh, relW,
								relH, offsetWidth, offsetHeight, null, false,
								true);
						AxisCoordinate last = null;
						for (int i = 0; i < 2; i++) {
							freeAxis = axis.freeAxis[i];
							freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW,
									relH, offsetWidth, offsetHeight, last,
									true, false);
							if (freeAxisOffset != INVALID) {
								break;
							}
							last = freeAxis;
						}
						if (freeAxisOffset == INVALID) {
							freeAxis = axis.freeAxis[0];
							freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW,
									relH, offsetWidth, offsetHeight, null,
									true, true);
						}
					}
					x = fixedAxis.isVertical() ? freeAxisOffset
							: fixedAxisOffset;
					y = freeAxis.isVertical() ? freeAxisOffset
							: fixedAxisOffset;
					// adjust to relative container
					x += boundingWidget.getAbsoluteLeft();
					y += boundingWidget.getAbsoluteTop();
					x -= positioningWidget.getAbsoluteLeft();
					y -= positioningWidget.getAbsoluteTop();
				}
				rpp.setPopupPosition(x, y);
			}
		});
		return rpp;
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
		public int preferredFromBottom;

		public MouseEvent mouseEvent;

		public RelativePopupAxis[] axes;

		public OtherPositioningStrategy positioningStrategy;

		public boolean shiftToEventXY = false;

		public int shiftY;

		public int preferredLeft;

		public int preferredTop;
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
				int ppW, int ppH, AxisCoordinate favour,
				boolean wrappingRelativeTo, boolean force) {
			int relC = relX;
			int bDim = bw;
			int relDim = relW;
			int ppDim = ppW;
			if (isVertical()) {
				relC = relY;
				bDim = bh;
				relDim = relH;
				ppDim = ppH;
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
						result = bDim - ppDim;// make as close to "left-align"
						// as poss
						break;
					case POS:
						result = 0;// as close to 'right-align' as poss
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
}
