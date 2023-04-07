package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import cc.alcina.framework.common.client.util.IntPair;

class MarkupMatch {
	List<IntPair> matches = new ArrayList<>();

	private String string;

	boolean emptyMatch = false;

	public MarkupMatch(String string, String value) {
		this.string = string;
		if (value.isEmpty()) {
			emptyMatch = true;
			return;
		}
		String matchString = string.toLowerCase();
		String matchValue = value.toLowerCase();
		int idx = 0;
		while (true) {
			int matchIdx = matchString.indexOf(matchValue, idx);
			if (matchIdx == -1) {
				break;
			} else {
				int end = matchIdx + value.length();
				matches.add(new IntPair(matchIdx, end));
				idx = end;
			}
		}
	}

	boolean hasMatches() {
		return emptyMatch || matches.size() > 0;
	}

	String toMarkup() {
		SafeHtmlBuilder builder = new SafeHtmlBuilder();
		int idx = 0;
		for (IntPair match : matches) {
			builder.appendEscaped(
					string.substring(idx, match.i1));
			builder.appendHtmlConstant("<match>");
			builder.appendEscaped(
					string.substring(match.i1, match.i2));
			builder.appendHtmlConstant("</match>");
			idx = match.i2;
		}
		builder.appendEscaped(string.substring(idx));
		return builder.toSafeHtml().asString();
	}
}