package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.gwt.client.objecttree.RenderContext;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel.PositionCallback;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class RelativePopupPositioning {
	public static final String RENDER_CONTEXT_BOUNDING_PARENT = "RENDER_CONTEXT_BOUNDING_PARENT";

	public static void setCurrentBoundingParent(Widget boundingParent) {
		RenderContext.current().set(RENDER_CONTEXT_BOUNDING_PARENT,
				boundingParent);
	}

	public static Widget getCurrentBoundingParent() {
		return RenderContext.current().get(RENDER_CONTEXT_BOUNDING_PARENT);
	}

	enum AxisType {
		NEG, CENTER, POS
	}

	private static int INVALID = -99999;

	enum AxisCoordinate {
		H_LEFT {
			@Override
			public boolean isVertical() {
				return false;
			}

			@Override
			public AxisType axisType() {
				return AxisType.NEG;
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

		abstract AxisType axisType();

		abstract boolean isVertical();
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

	public static RelativePopupPanel showPopup(Widget relativeToWidget,
			Widget widgetToShow, Widget boundingWidget, RelativePopupAxis axis) {
		return showPopup(relativeToWidget, widgetToShow, boundingWidget, axis,
				null,null);
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
		if (widgetToShow != null) {
			rpp.setWidget(widgetToShow);
		}
		final Widget positioningWidget = relativeContainer != null ? relativeContainer
				: WidgetUtils.getPositioningParent(relativeToWidget);
		ComplexPanel cp = WidgetUtils.complexChildOrSelf(positioningWidget);
		rpp.setPositioningContainer(cp);
		rpp.setPopupPositionAndShow(new PositionCallback() {
			public void setPosition(int offsetWidth, int offsetHeight) {
				int x = relativeToWidget.getAbsoluteLeft();
				int y = relativeToWidget.getAbsoluteTop();
				int relW = relativeToWidget.getOffsetWidth();
				int relH = relativeToWidget.getOffsetHeight();
				if(relH==0){
					Element parentElement = relativeToWidget.getElement().getParentElement();
					relH=parentElement.getOffsetHeight();
					y=parentElement.getAbsoluteTop();
				}
				x += shiftX;
				y += shiftY;
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
				for (RelativePopupAxis axis : axes) {
					fixedAxis = axis.fixedAxis;
					fixedAxisOffset = fixedAxis.fit(x, y, bw, bh, relW, relH,
							offsetWidth, offsetHeight, null, false, false);
					if (fixedAxisOffset == INVALID) {
						continue;
					}
					AxisCoordinate last = null;
					for (int i = 0; i < 2; i++) {
						freeAxis = axis.freeAxis[i];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, last, true, false);
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
							offsetWidth, offsetHeight, null, false, true);
					AxisCoordinate last = null;
					for (int i = 0; i < 2; i++) {
						freeAxis = axis.freeAxis[i];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, last, true, false);
						if (freeAxisOffset != INVALID) {
							break;
						}
						last = freeAxis;
					}
					if (freeAxisOffset == INVALID) {
						freeAxis = axis.freeAxis[0];
						freeAxisOffset = freeAxis.fit(x, y, bw, bh, relW, relH,
								offsetWidth, offsetHeight, null, true, true);
					}
				}
				x = fixedAxis.isVertical() ? freeAxisOffset : fixedAxisOffset;
				y = freeAxis.isVertical() ? freeAxisOffset : fixedAxisOffset;
				// adjust to relative container
				x += boundingWidget.getAbsoluteLeft();
				y += boundingWidget.getAbsoluteTop();
				x -= positioningWidget.getAbsoluteLeft();
				y -= positioningWidget.getAbsoluteTop();
				rpp.setPopupPosition(x, y);
			}
		});
		return rpp;
	}
}
