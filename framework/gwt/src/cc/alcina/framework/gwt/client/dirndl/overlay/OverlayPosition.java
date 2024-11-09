package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;

import cc.alcina.framework.common.client.util.DoublePair;
import cc.alcina.framework.common.client.util.FormatBuilder;

/**
 * Models a set of positioning constraints between a DomRect and an element (or
 * the viewport and an element), and can position the element to enforce the
 * constraint. Effectively like a graphics app, 'align start to end' etc - this
 * is a decent model for popup layout.
 *
 * 
 *
 */
public class OverlayPosition {
	boolean viewportCentered;

	DomRect fromRect;

	List<Constraint> constraints = new ArrayList<>();

	Element toElement;

	DomRect toRect;

	public void addConstraint(Direction direction, Position from, Position to,
			int px) {
		constraints.add(new Constraint(direction, from, to, px));
	}

	void apply() {
		// allow exactly one of {viewportCentered,non-empty constraints}
		Preconditions.checkState(viewportCentered ^ constraints.size() > 0);
		if (viewportCentered) {
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
	}

	public void dropdown(Position xalign, DomRect rect) {
		fromRect(rect);
		addConstraint(Direction.X_AXIS, xalign, xalign, 0);
		addConstraint(Direction.Y_AXIS, Position.END, Position.START, 6);
	}

	public OverlayPosition fromRect(DomRect fromRect) {
		this.fromRect = fromRect;
		return this;
	}

	public OverlayPosition toElement(Element toElement) {
		this.toElement = toElement;
		return this;
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("viewportCentered", viewportCentered,
				"constraints", constraints);
	}

	/*
	 * Note - viewport-centered is rendered via css, not style/coordinate
	 * modification.
	 */
	public OverlayPosition viewportCentered(boolean viewportCentered) {
		this.viewportCentered = viewportCentered;
		return this;
	}

	class Constraint {
		Direction direction;

		int px;

		Position from;

		Position to;

		Constraint(Direction direction, Position from, Position to, int px) {
			this.direction = direction;
			this.from = from;
			this.to = to;
			this.px = px;
		}

		void apply() {
			DoublePair fromLine = line(fromRect);
			DoublePair toLine = line(toRect);
			double fromOffset = pos(fromLine, from);
			double toOffset = pos(toLine, to);
			double delta = fromOffset - toOffset + px;
			move(delta);
		}

		private DoublePair line(DomRect rect) {
			switch (direction) {
			case X_AXIS:
				return new DoublePair(rect.left, rect.right);
			case Y_AXIS:
				return new DoublePair(rect.top, rect.bottom);
			default:
				throw new UnsupportedOperationException();
			}
		}

		void move(double offset) {
			switch (direction) {
			case X_AXIS:
				toElement.getStyle().setLeft(offset, Unit.PX);
				break;
			case Y_AXIS:
				toElement.getStyle().setTop(offset, Unit.PX);
				break;
			default:
				throw new UnsupportedOperationException();
			}
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

		private double pos(DoublePair line, Position position) {
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
			return FormatBuilder.keyValues("direction", direction, "px", px,
					"from", from, "to", to);
		}
	}

	enum Direction {
		X_AXIS, Y_AXIS
	}

	public enum Position {
		START, CENTER, END
	}
}
