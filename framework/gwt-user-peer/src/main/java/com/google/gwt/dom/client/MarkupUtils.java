package com.google.gwt.dom.client;

public class MarkupUtils {
	public static String expandEmptyElements(String s) {
		// optimised
		// String regex = "<([a-zA-Z0-9_\\-]+)([^<]*?)/>";
		// Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL
		// | Pattern.CASE_INSENSITIVE);
		// Matcher m = p.matcher(s);
		// s = m.replaceAll("<$1$2></$1>");
		// return s;
		StringBuilder s2 = new StringBuilder((int) (s.length() * 1.1));
		int l = s.length() - 1;
		int tagStart = -1;
		int tagEnd = -1;
		int idx = 0;
		for (; idx < l; idx++) {
			boolean app = true;
			char c = s.charAt(idx);
			char c2 = s.charAt(idx + 1);
			if (c == '<') {
				tagEnd = -1;
				if (c2 != '/') {
					tagStart = idx + 1;
				} else {
					tagStart = -1;
				}
			} else if (tagStart != -1) {
				if (c == '/' && c2 == '>') {
					s2.append("></");
					tagEnd = tagEnd == -1 ? idx : tagEnd;
					for (int j = tagStart; j < tagEnd; j++) {
						s2.append(s.charAt(j));
					}
					s2.append(">");
					idx++;
					app = false;
				} else if (tagEnd == -1) {
					if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
							|| (c >= '0' && c <= '9') || c == '_' || c == '-') {
					} else {
						tagEnd = idx;
					}
				}
			}
			if (app) {
				s2.append(c);
			}
		}
		if (idx == l) {
			s2.append(s.charAt(idx));
		}
		return s2.toString();
	}

	public static String simpleInnerText(String innerHTML) {
		int idx = 0, idy = 0, x1, x2, min;
		StringBuilder result = new StringBuilder();
		while (true) {
			x1 = innerHTML.indexOf("<", idx);
			x2 = innerHTML.indexOf("&", idx);
			if (x1 == -1 && x2 == -1) {
				break;
			}
			min = x1 != -1 && (x2 == -1 || x1 < x2) ? x1 : x2;
			result.append(innerHTML.substring(idx, min));
			if (min == x1) {
				x2 = innerHTML.indexOf(">", min);
				if (x2 == -1) {
					// invalidish html, bail
					break;
				} else {
					idx = x2 + 1;
				}
			} else {
				x2 = innerHTML.indexOf(";", min);
				if (x2 == -1) {
					// invalidish html, bail
					break;
				} else {
					String minStr = innerHTML.substring(min + 1, x2);
					if (minStr.equals("amp")) {
						result.append("&");
					} else if (minStr.equals("lt")) {
						result.append("<");
					} else if (minStr.equals("gt")) {
						result.append(">");
					} else {
						try {
							int cc = Integer.parseInt(minStr);
							result.append((char) cc);
						} catch (NumberFormatException ne) {
							// ignore
						}
					}
					idx = x2 + 1;
				}
			}
		}
		result.append(innerHTML.substring(idx));
		return result.toString();
	}
}
