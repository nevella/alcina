package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;

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

	public static String normalise(String text) {
		return TextUtilsImpl.normalise(text);
	}

	public static List<IntPair> match(String text, String regex) {
		if (text == null || regex == null) {
			return new ArrayList<IntPair>();
		}
		return TextUtilsImpl.match(text, regex);
	}

	public static boolean isWhitespaceOrEmpty(String text) {
		return CommonUtils.isNullOrEmpty(text)
				|| normaliseAndTrim(text).length() == 0;
	}

	public static String normalisedLcKey(String key) {
		return normaliseAndTrim(CommonUtils.nullToEmpty(key).toLowerCase());
	}

	public static native void setElementStyle(Element e, String css) /*-{
        if (e.style && typeof (e.style.cssText) == "string") {
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

	public static int getWordCount(String data) {
		String normalised = normaliseAndTrim(data);
		return normalised.length() == 0 ? 0 : normalised.split(" ").length;
	}

	public static List<IntPair> findStringMatches(String text, String search) {
		int idx0 = 0;
		List<IntPair> result = new ArrayList<>();
		while (true) {
			int idx1 = text.indexOf(search, idx0);
			if (idx1 == -1) {
				break;
			} else {
				result.add(new IntPair(idx1, idx1 + search.length()));
				idx0 = idx1 + search.length();
			}
		}
		return result;
	}
}
