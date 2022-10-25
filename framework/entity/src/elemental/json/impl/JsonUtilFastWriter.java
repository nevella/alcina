/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.json.impl;

import cc.alcina.framework.entity.util.LengthConstrainedStringBuilder;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@SuppressWarnings("deprecation")
class JsonUtilFastWriter {
	/**
	 * Convert special control characters into unicode escape format.
	 */
	public static String escapeControlChars(String text) {
		StringBuilder toReturn = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (isControlChar(c)) {
				toReturn.append(escapeCharAsUnicode(c));
			} else {
				toReturn.append(c);
			}
		}
		return toReturn.toString();
	}

	/**
	 * Converts a Json object to Json formatted String.
	 *
	 * @param jsonValue
	 *            json object to stringify
	 * @return json formatted string
	 */
	public static String stringify(JsonValue jsonValue) {
		final LengthConstrainedStringBuilder builder = new LengthConstrainedStringBuilder();
		new Visitor(builder).accept(jsonValue);
		return builder.toString();
	}

	/**
	 * Turn a single unicode character into a 32-bit unicode hex literal.
	 */
	private static String escapeCharAsUnicode(char toEscape) {
		String hexValue = Integer.toString(toEscape, 16);
		int padding = 4 - hexValue.length();
		return "\\u" + ("0000".substring(0, padding)) + hexValue;
	}

	private static boolean isControlChar(char c) {
		return (c >= 0x00 && c <= 0x1f) || (c >= 0x7f && c <= 0x9f)
				|| c == '\u00ad' || c == '\u070f' || c == '\u17b4'
				|| c == '\u17b5' || c == '\ufeff'
				|| (c >= '\u0600' && c <= '\u0604')
				|| (c >= '\u200c' && c <= '\u200f')
				|| (c >= '\u2028' && c <= '\u202f')
				|| (c >= '\u2060' && c <= '\u206f')
				|| (c >= '\ufff0' && c <= '\uffff');
	}

	private static class Visitor extends JsonVisitor {
		private final LengthConstrainedStringBuilder builder;

		private Visitor(LengthConstrainedStringBuilder builder) {
			this.builder = builder;
		}

		@Override
		public void endVisit(JsonArray array, JsonContext ctx) {
			builder.append(']');
		}

		@Override
		public void endVisit(JsonObject object, JsonContext ctx) {
			builder.append('}');
		}

		@Override
		public void visit(boolean bool, JsonContext ctx) {
			builder.append(bool);
		}

		@Override
		public void visit(double number, JsonContext ctx) {
			builder.append(
					Double.isInfinite(number) || Double.isNaN(number) ? "null"
							: format(number));
		}

		@Override
		public boolean visit(JsonArray array, JsonContext ctx) {
			builder.append('[');
			return true;
		}

		@Override
		public boolean visit(JsonObject object, JsonContext ctx) {
			builder.append('{');
			return true;
		}

		@Override
		public void visit(String string, JsonContext ctx) {
			quote(string);
		}

		@Override
		public boolean visitIndex(int index, JsonContext ctx) {
			commaIfNotFirst(ctx);
			return true;
		}

		@Override
		public boolean visitKey(String key, JsonContext ctx) {
			commaIfNotFirst(ctx);
			// key is a java identifier, so do not quote
			builder.append("\"");
			builder.append(key);
			builder.append("\":");
			return true;
		}

		@Override
		public void visitNull(JsonContext ctx) {
			builder.append("null");
		}

		private void commaIfNotFirst(JsonContext ctx) {
			if (!ctx.isFirst()) {
				builder.append(',');
			}
		}

		private String format(double number) {
			String n = String.valueOf(number);
			if (n.endsWith(".0")) {
				n = n.substring(0, n.length() - 2);
			}
			return n;
		}

		/**
		 * Safely escape an arbitrary string as a JSON string literal.
		 */
		void quote(String value) {
			if (value.contains("ViewEvent$Data")) {
				int debug = 3;
			}
			builder.append('\"');
			int length = value.length();
			for (int i = 0; i < length; i++) {
				char c = value.charAt(i);
				if (c >= 0x20 && c < 0x7F && c != '"' && c != '\\') {
					builder.append(c);
					continue;
				}
				String toAppend = null;
				switch (c) {
				case '\b':
					toAppend = "\\b";
					break;
				case '\t':
					toAppend = "\\t";
					break;
				case '\n':
					toAppend = "\\n";
					break;
				case '\f':
					toAppend = "\\f";
					break;
				case '\r':
					toAppend = "\\r";
					break;
				case '"':
					toAppend = "\\\"";
					break;
				case '\\':
					toAppend = "\\\\";
					break;
				default:
					if (isControlChar(c)) {
						toAppend = escapeCharAsUnicode(c);
					} else {
						toAppend = String.valueOf(c);
					}
				}
				builder.append(toAppend);
			}
			builder.append('"');
		}
	}
}
