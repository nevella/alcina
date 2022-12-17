package cc.alcina.framework.gwt.client.util;

import java.util.function.Predicate;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;

public class StyleUtil {
	public static Query query(Element from) {
		return new Query(from);
	}

	public static Action action(Element target) {
		return new Action(target);
	}

	public static class Action {
		private Element target;

		private boolean smoothScroll;

		public Action(Element target) {
			this.target = target;
		}

		public Action withSmoothScroll(boolean smoothScroll) {
			this.smoothScroll = smoothScroll;
			return this;
		}

		public void ensureDistanceFromViewportBottom(int px) {
			// there may well be a modern DOM way to do this...
			int top = target.getAbsoluteTop();
			int viewportTop = Window.getScrollTop();
			int viewportHeight = Window.getClientHeight();
			int fromTop = top - viewportTop;
			if (px < viewportHeight && fromTop > 0 && fromTop < viewportHeight
					&& fromTop + px > viewportHeight) {
				int to = viewportTop + ((fromTop + px) - viewportHeight);
				Window.scrollTo(Window.getScrollLeft(), to, smoothScroll);
			}
		}
	}

	public static class Query {
		boolean block;

		boolean positioningContainer;

		private Element from;

		public Query(Element from) {
			this.from = from;
		}

		public Query withBlock(boolean block) {
			this.block = block;
			return this;
		}

		public Query withPositioningContainer(boolean positioningContainer) {
			this.positioningContainer = positioningContainer;
			return this;
		}

		public Element execute() {
			Element cursor = from;
			Predicate<Element> p = e -> true;
			p = p.and(e -> !block ^ (WidgetUtils
					.getComputedStyle(e, "visibility").equals("block")
					|| WidgetUtils.getComputedStyle(e, "visibility")
							.equals("flex")));
			p = p.and(e -> !positioningContainer ^ (WidgetUtils
					.getComputedStyle(e, "position").equals("relative")
					|| WidgetUtils.getComputedStyle(e, "position")
							.equals("absolute")));
			do {
				if (p.test(cursor)) {
					return cursor;
				}
				cursor = cursor.getParentElement();
			} while (cursor != null);
			return null;
		}
	}
}
