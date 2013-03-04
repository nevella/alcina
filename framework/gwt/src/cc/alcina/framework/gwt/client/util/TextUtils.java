package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class TextUtils {
	public static String normaliseAndTrim(String text) {
		return TextUtilsImpl.normalise(text).trim();
	}

	public static boolean isWhitespaceOrEmpty(String text) {
		return CommonUtils.isNullOrEmpty(text)
				|| normaliseAndTrim(text).length() == 0;
	}

	public static String normalisedLcKey(String key) {
		return normaliseAndTrim(CommonUtils.nullToEmpty(key).toLowerCase());
	}

	public static native void setElementStyle(Element e, String css) /*-{
		if (e.style && typeof (e.style.cssText)=="string") {
			e.style.cssText = css;
		} else {
			e.style = css;
		}
	}-*/;

	public static String trimToWidth(String s, String style, int pxWidth,
			String ellipsis) {
		if (pxWidth <= 20) {
			return s;
		}
		ellipsis = ellipsis == null ? "\u2026" : ellipsis;
		int r0 = 0;
		int r1 = s.length();
		Label l = new Label();
		setElementStyle(l.getElement(), style);
		Style cStyle = l.getElement().getStyle();
		cStyle.setPosition(Position.ABSOLUTE);
		cStyle.setLeft(0, Unit.PX);
		cStyle.setTop(0, Unit.PX);
		cStyle.setDisplay(Display.INLINE_BLOCK);
		cStyle.setProperty("whitespace", "nowrap");
		cStyle.setProperty("visibility", "hidden");
		RootPanel.get().add(l);
		boolean tried = false;
		while (true) {
			int mid = (r1 - r0) / 2 + r0;
			String t = tried ? s.substring(0, mid) + ellipsis : s;
			l.setText(t);
			if (l.getOffsetWidth() <= pxWidth) {
				if (!tried || (r1 - r0) <= 1) {
					RootPanel.get().remove(l);
					return t;
				}
				r0 = mid;
			} else {
				if (!tried) {
					tried = true;
				} else {
					r1 = mid;
				}
			}
		}
	}
}
