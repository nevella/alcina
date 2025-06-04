package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.DoublePair;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * Models a set of positioning constraints between a DomRect and an element (or
 * the viewport and an element), and can position the element to enforce the
 * constraint. Effectively like a graphics app, 'align start to end' etc - this
 * is a decent model for popup layout.
 *
 * 
 *
 */
/*
 * FIX - auto tracking of source rect-relative positioning via mutation observer
 */
public class OverlayPosition {
	@Reflected
	public enum ViewportRelative {
		TOP_LEFT, TOP_CENTER, TOP_RIGHT, //
		MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, //
		BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;
	}

	ViewportRelative viewportRelative;

	DomRect fromRect;

	List<Constraint> constraints = new ArrayList<>();

	Element toElement;

	DomRect toRect;

	ViewportConstraint viewportConstraint = ViewportConstraint.ATTEMPT_VISIBLE;

	Element rectSourceElement;

	@Reflected
	public enum ViewportConstraint {
		ATTEMPT_VISIBLE {
			/*
			 * See discussion/outline in the package javadoc
			 * 
			 * Note that this assumes the dropdown is positioned below fromRect
			 */
			@Override
			void apply(OverlayPosition overlayPosition) {
				DomRect viewport = Window.getRect();
				DomRect toRect = overlayPosition.toElement
						.getBoundingClientRect();
				/*
				 * x
				 */
				DoublePair viewportXRange = viewport.xRange();
				DoublePair toXRange = toRect.xRange();
				if (viewportXRange.contains(toXRange)) {
					// ok
				} else {
					if (viewportXRange.length() < toXRange.length()) {
						// nothing to do - unmanageable overflow
					} else {
						if (viewport.left > toRect.left) {
							overlayPosition.set(0, Direction.X_AXIS);
						} else {
							overlayPosition.set(viewport.width - toRect.width,
									Direction.X_AXIS);
						}
					}
				}
				/*
				 * y
				 */
				DoublePair viewportYRange = viewport.yRange();
				DoublePair toYRange = toRect.yRange();
				if (viewportYRange.contains(toYRange)) {
					// ok
				} else {
					// WIP -
					if (viewportYRange.length() < toYRange.length()) {
						// nothing to do - unmanageable overflow
					} else {
						/*
						 * WIP (need to determine the highest/lowest that rect
						 * wd be visible given scroll containers) /*
						 * 
						 * For the moment, just position above
						 */
						double to = overlayPosition.fromRect.top - toRect.height
								- overlayPosition
										.constraint(Direction.Y_AXIS).paddingPx;
						if (to >= 0) {
							overlayPosition.set(to, Direction.Y_AXIS);
						}
					}
				}
			}
		},
		NONE {
			@Override
			void apply(OverlayPosition overlayPosition) {
			}
		};

		abstract void apply(OverlayPosition overlayPosition);
	}

	Constraint constraint(Direction axis) {
		return constraints.stream().filter(c -> c.direction == axis).findFirst()
				.get();
	}

	public void addConstraint(Direction direction, Position from, Position to,
			int px) {
		constraints.add(new Constraint(direction, from, to, px));
	}

	void apply() {
		// allow exactly one of {viewportRelative,non-empty constraints}
		Preconditions
				.checkState(viewportRelative != null ^ constraints.size() > 0);
		if (viewportRelative != null) {
			return;
		}
		Preconditions.checkState(fromRect != null);
		if (constraints.stream().noneMatch(Constraint::requiresActualToRect)) {
			// will return a [0,0,0,0] domRect - which is fine if positioning
			// non-center constraints
			toRect = new DomRect();
		} else {
			toRect = toElement.getBoundingClientRect();
		}
		constraints.forEach(Constraint::apply);
		/*
		 * FIXME - romcom - this should be run client-side (non-romcom, better
		 * to not defer)
		 */
		Client.eventBus().queued().lambda(() -> viewportConstraint.apply(this))
				.deferred().dispatch();
	}

	public void dropdown(Position xalign, DomRect rect, Model rectSource,
			int yOffset) {
		if (rect == null) {
			Element rectSourceElement = rectSource.provideNode().getRendered()
					.asElement();
			rect = rectSourceElement.getBoundingClientRect();
			withRectSourceElement(rectSourceElement);
		}
		withFromRect(rect);
		addConstraint(Direction.X_AXIS, xalign, xalign, 0);
		addConstraint(Direction.Y_AXIS, Position.END, Position.START, yOffset);
	}

	public OverlayPosition withFromRect(DomRect fromRect) {
		this.fromRect = fromRect;
		return this;
	}

	public OverlayPosition withRectSourceElement(Element rectSourceElement) {
		this.rectSourceElement = rectSourceElement;
		return this;
	}

	public OverlayPosition withToElement(Element toElement) {
		this.toElement = toElement;
		return this;
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("viewportRelative", viewportRelative,
				"constraints", constraints);
	}

	public OverlayPosition viewportCentered() {
		return withViewportRelative(ViewportRelative.MIDDLE_CENTER);
	}

	/*
	 * Note - viewport-cenrelativetered is rendered via css, not
	 * style/coordinate modification.
	 */
	public OverlayPosition
			withViewportRelative(ViewportRelative viewportRelative) {
		this.viewportRelative = viewportRelative;
		return this;
	}

	public OverlayPosition
			withViewportConstraint(ViewportConstraint viewportConstraint) {
		this.viewportConstraint = viewportConstraint;
		return this;
	}

	void set(double absolutePx, Direction direction) {
		switch (direction) {
		case X_AXIS:
			toElement.getStyle().setLeft(absolutePx, Unit.PX);
			break;
		case Y_AXIS:
			toElement.getStyle().setTop(absolutePx, Unit.PX);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	class Constraint {
		Direction direction;

		int paddingPx;

		Position from;

		Position to;

		Constraint(Direction direction, Position from, Position to,
				int paddingPx) {
			this.direction = direction;
			this.from = from;
			this.to = to;
			this.paddingPx = paddingPx;
		}

		void apply() {
			DoublePair fromLine = line(fromRect);
			DoublePair toLine = line(toRect);
			double fromOffset = pos(fromLine, from);
			double toOffset = pos(toLine, to);
			double delta = fromOffset - toOffset + paddingPx;
			set(delta);
		}

		private DoublePair line(DomRect rect) {
			switch (direction) {
			case X_AXIS:
				return rect.xRange();
			case Y_AXIS:
				return rect.yRange();
			default:
				throw new UnsupportedOperationException();
			}
		}

		void set(double offset) {
			OverlayPosition.this.set(offset, direction);
		}

		boolean requiresActualToRect() {
			switch (to) {
			case CENTER:
			case END:
				return true;
			case START:
				return false;
			default:
				throw new UnsupportedOperationException();
			}
		}

		double pos(DoublePair line, Position position) {
			switch (position) {
			case START:
				return line.d1;
			case END:
				return line.d2;
			case CENTER:
				return (line.d2 + line.d1) / 2;
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("direction", direction, "px",
					paddingPx, "from", from, "to", to);
		}
	}

	enum Direction {
		X_AXIS, Y_AXIS
	}

	public enum Position {
		START, CENTER, END
	}

	/**
	 * Return a positiong rect for a click on a potentially multiline text
	 * 
	 * @param originatingNativeEvent
	 * @return
	 */
	public static DomRect
			getTextClickRelativeRect(NativeEvent originatingNativeEvent) {
		if (originatingNativeEvent == null) {
			return null;
		}
		double x = originatingNativeEvent.getClientX();
		double y = originatingNativeEvent.getClientY();
		return DomRect.ofCoordinatePairs(x - 20, y - 20, x + 20, y + 20);
	}

	public void overlay(Model rectSource) {
		Element rectSourceElement = rectSource.provideNode().getRendered()
				.asElement();
		DomRect rect = rectSourceElement.getBoundingClientRect();
		withRectSourceElement(rectSourceElement);
		withFromRect(rect);
		addConstraint(Direction.X_AXIS, Position.START, Position.START, 0);
		addConstraint(Direction.Y_AXIS, Position.START, Position.START, 0);
	}
}
