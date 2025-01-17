package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.StringMap;

public class StyleUtil {
	public static Action action(Element target) {
		return new Action(target);
	}

	public static Query query(Element from) {
		return new Query(from);
	}

	public static class Action {
		private Element target;

		private boolean smoothScroll;

		public Action(Element target) {
			this.target = target;
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

		public Action withSmoothScroll(boolean smoothScroll) {
			this.smoothScroll = smoothScroll;
			return this;
		}
	}

	public static class Query {
		boolean block;

		boolean positioningContainer;

		private Element from;

		public Query(Element from) {
			this.from = from;
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

		public Query withBlock(boolean block) {
			this.block = block;
			return this;
		}

		public Query withPositioningContainer(boolean positioningContainer) {
			this.positioningContainer = positioningContainer;
			return this;
		}
	}

	public static ClassAttributeBuilder classAttribute() {
		return new ClassAttributeBuilder();
	}

	public static class ClassAttributeBuilder {
		List<String> names = new ArrayList<>();

		public ClassAttributeBuilder add(String name) {
			this.names.add(name);
			return this;
		}

		public ClassAttributeBuilder add(List<String> names) {
			this.names.addAll(names);
			return this;
		}

		public String classAttributeValue() {
			return names.stream().collect(Collectors.joining(" "));
		}
	}

	public static StringMap styleAttributeToMap(String styleAttributeValue) {
		return StringMap.fromPropertyString(styleAttributeValue
				.replaceAll(":\\s*", "=").replaceAll(";\n*", "\n"));
	}

	public static String styleMapToAttribute(StringMap styleMap) {
		return styleMap.toPropertyString().replaceAll("=", ": ")
				.replaceAll("\n+", ";") + ";";
	}
}
