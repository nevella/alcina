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
package com.google.gwt.dom.client;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlHostedModeUtils;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.safehtml.shared.annotations.SuppressIsSafeHtmlCastCheck;

/**
 * A builder that facilitates the building up of XSS-safe HTML from text
 * snippets. It is used essentially like a {@link StringBuilder}; unlike a
 * {@link StringBuilder}, it automatically HTML-escapes appended input where
 * necessary.
 *
 * <p>
 * In addition, it supports methods that allow strings with HTML markup to be
 * appended without escaping: One can append other {@link SafeHtml} objects, and
 * one can append constant strings. The method that appends constant strings (
 * {@link #appendHtmlConstant(String)}) requires a convention of use to be
 * adhered to in order for this class to adhere to the contract required by
 * {@link SafeHtml}: The argument expression must be fully determined and known
 * to be safe at compile time, and the value of the argument must not contain
 * incomplete HTML tags. See {@link #appendHtmlConstant(String)} for details.
 *
 * <p>
 * The accumulated XSS-safe HTML can be obtained in the form of a
 * {@link SafeHtml} via the {@link #toSafeHtml()} method.
 * 
 * <p>
 * (Nick) - called unsafehtmlbuilder for a reason...
 *
 * <p>
 * This class is not thread-safe.
 */
public final class UnsafeHtmlBuilder {
	static RegExp unsafeTest = RegExp.compile("[&<>'\"]");

	static RegExp unsafeTestNoQuotes = RegExp.compile("[&<>]");

	private final StringBuilder sb = new StringBuilder();

	/*
	 * If true, tags such as <img> will not be closed - so <img src='xx'>, not
	 * <img src='xx' />
	 */
	final boolean htmlTags;

	final boolean pretty;

	int depth;

	/**
	 * Constructs an empty UnsafeSafeHtmlBuilder.
	 * 
	 * @param pretty
	 */
	public UnsafeHtmlBuilder(boolean htmlTags, boolean pretty) {
		this.htmlTags = htmlTags;
		this.pretty = pretty;
	}

	/*
	 * Boolean and numeric types converted to String are always HTML safe -- no
	 * escaping necessary.
	 */
	/**
	 * Appends the string representation of a boolean.
	 *
	 * @param b
	 *            the boolean whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(boolean b) {
		sb.append(b);
		return this;
	}

	/**
	 * Appends the string representation of a number.
	 *
	 * @param num
	 *            the number whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(byte num) {
		sb.append(num);
		return this;
	}

	/**
	 * Appends the string representation of a char.
	 *
	 * @param c
	 *            the character whose string representation to append
	 * @return a reference to this object
	 * @see SafeHtmlUtils#htmlEscape(char)
	 */
	public UnsafeHtmlBuilder append(char c) {
		sb.append(SafeHtmlUtils.htmlEscape(c));
		return this;
	}

	/**
	 * Appends the string representation of a number.
	 *
	 * @param num
	 *            the number whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(double num) {
		sb.append(num);
		return this;
	}

	/**
	 * Appends the string representation of a number.
	 *
	 * @param num
	 *            the number whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(float num) {
		sb.append(num);
		return this;
	}

	/**
	 * Appends the string representation of a number.
	 *
	 * @param num
	 *            the number whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(int num) {
		sb.append(num);
		return this;
	}

	/**
	 * Appends the string representation of a number.
	 *
	 * @param num
	 *            the number whose string representation to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(long num) {
		sb.append(num);
		return this;
	}

	/**
	 * Appends the contents of another {@link SafeHtml} object, without applying
	 * HTML-escaping to it.
	 *
	 * @param html
	 *            the {@link SafeHtml} to append
	 * @return a reference to this object
	 */
	public UnsafeHtmlBuilder append(SafeHtml html) {
		sb.append(html.asString());
		return this;
	}

	/**
	 * Appends a string after HTML-escaping it.
	 *
	 * @param text
	 *            the string to append
	 * @return a reference to this object
	 * @see SafeHtmlUtils#htmlEscape(String)
	 */
	public UnsafeHtmlBuilder appendEscaped(String text) {
		if (text != null) {
			if (unsafeTest.exec(text) == null) {
				sb.append(text);
			} else {
				sb.append(SafeHtmlUtils.htmlEscape(text));
			}
		}
		return this;
	}

	/**
	 * Appends a string consisting of several newline-separated lines after
	 * HTML-escaping it. Newlines in the original string are converted to {@code
	 * <br>
	} tags.
	 *
	 * @param text
	 *            the string to append
	 * @return a reference to this object
	 * @see SafeHtmlUtils#htmlEscape(String)
	 */
	public UnsafeHtmlBuilder appendEscapedLines(String text) {
		sb.append(SafeHtmlUtils.htmlEscape(text).replaceAll("\n", "<br>"));
		return this;
	}

	public UnsafeHtmlBuilder appendEscapedNoQuotes(String text) {
		if (text != null) {
			if (unsafeTestNoQuotes.exec(text) == null) {
				sb.append(text);
			} else {
				sb.append(SafeHtmlUtils.htmlEscapeNoQuotes(text));
			}
		}
		return this;
	}

	/**
	 * Appends a compile-time-constant string, which will <em>not</em> be
	 * escaped.
	 *
	 * <p>
	 * <b>Important</b>: For this class to be able to honor its contract as
	 * required by {@link SafeHtml}, all uses of this method must satisfy the
	 * following constraints:
	 *
	 * <ol>
	 *
	 * <li>The argument expression must be fully determined at compile time.
	 *
	 * <li>The value of the argument must end in "inner HTML" context and not
	 * contain incomplete HTML tags. I.e., the following is not a correct use of
	 * this method, because the {@code <a>} tag is incomplete:
	 *
	 * <pre class="code">
	 * {@code
	 * shb.appendHtmlConstant("<a href='").append(url)
	 * }
	 * </pre>
	 *
	 * </ol>
	 *
	 * <p>
	 * The first constraint provides a sufficient condition that the appended
	 * string (and any HTML markup contained in it) originates from a trusted
	 * source. The second constraint ensures the composability of
	 * {@link SafeHtml} values.
	 *
	 * <p>
	 * When executing client-side in Development Mode, or server-side with
	 * assertions enabled, the argument is HTML-parsed and validated to satisfy
	 * the second constraint (the server-side check can also be enabled
	 * programmatically, see
	 * {@link SafeHtmlHostedModeUtils#maybeCheckCompleteHtml(String)} for
	 * details). For performance reasons, this check is not performed in prod
	 * mode on the client, and with assertions disabled on the server.
	 *
	 * @param html
	 *            the HTML snippet to be appended
	 * @return a reference to this object
	 * @throws IllegalArgumentException
	 *             if not running in prod mode and {@code
	 *           html} violates the second constraint
	 */
	public UnsafeHtmlBuilder appendHtmlConstant(String html) {
		SafeHtmlHostedModeUtils.maybeCheckCompleteHtml(html);
		sb.append(html);
		return this;
	}

	public UnsafeHtmlBuilder appendHtmlConstantNoCheck(String html) {
		sb.append(html);
		return this;
	}

	public UnsafeHtmlBuilder appendUnsafeHtml(String html) {
		sb.append(html);
		return this;
	}

	/**
	 * Returns the safe HTML accumulated in the builder as a {@link SafeHtml}.
	 *
	 * @return a SafeHtml instance
	 */
	public SafeHtml toSafeHtml() {
		return new SafeHtmlString(sb.toString());
	}

	static class SafeHtmlString implements SafeHtml {
		private String html;

		/**
		 * No-arg constructor for compatibility with GWT serialization.
		 */
		@SuppressWarnings("unused")
		private SafeHtmlString() {
		}

		/**
		 * Constructs a {@link SafeHtmlString} from a string. Callers are
		 * responsible for ensuring that the string passed as the argument to
		 * this constructor satisfies the constraints of the contract imposed by
		 * the {@link SafeHtml} interface.
		 *
		 * @param html
		 *            the string to be wrapped as a {@link SafeHtml}
		 */
		SafeHtmlString(String html) {
			if (html == null) {
				throw new NullPointerException("html is null");
			}
			this.html = html;
		}

		/**
		 * {@inheritDoc}
		 */
		@IsSafeHtml
		@SuppressIsSafeHtmlCastCheck
		public String asString() {
			return html;
		}

		/**
		 * Compares this string to the specified object.
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SafeHtml)) {
				return false;
			}
			return html.equals(((SafeHtml) obj).asString());
		}

		/**
		 * Returns a hash code for this string.
		 */
		@Override
		public int hashCode() {
			return html.hashCode();
		}

		@Override
		public String toString() {
			return "safe: \"" + asString() + "\"";
		}
	}

	public void modifyDepth(int i) {
		depth += i;
		if (pretty) {
			sb.append('\n');
			for (int idx = 0; idx < depth; idx++) {
				sb.append(' ');
			}
		}
	}
}
