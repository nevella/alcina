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
package com.google.gwt.regexp.shared;

import com.google.gwt.core.shared.GWT;

/**
 * GWT wrapper for the Javascript RegExp class extended with the Javascript
 * String class's replace and split methods, which can take a RegExp parameter.
 */
public class RegExp {
	private static Boolean isScript;

	public static RegExp compile(String pattern) {
		return isScript() ? RegExp_Jso.compile(pattern)
				: RegExp_Jvm.compile(pattern);
	}

	/**
	 * Creates a regular expression object from a pattern with no flags.
	 *
	 * @param pattern
	 *            the Javascript regular expression pattern to compile
	 * @param flags
	 *            the flags string, containing at most one occurence of {@code
	 *          'g'} ({@link #getGlobal()}), {@code 'i'} (
	 *            {@link #getIgnoreCase()} ), or {@code 'm'} (
	 *            {@link #getMultiline()}).
	 * @return a new regular expression
	 * @throws RuntimeException
	 *             if the pattern or the flags are invalid
	 */
	public static RegExp compile(String pattern, String flags) {
		return isScript() ? RegExp_Jso.compile(pattern, flags)
				: RegExp_Jvm.compile(pattern, flags);
	}

	/**
	 * Returns a literal pattern <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>
	 * This method produces a <code>String</code> that can be used to create a
	 * <code>RegExp</code> that would match the string <code>s</code> as if it
	 * were a literal pattern.
	 * </p>
	 * Metacharacters or escape sequences in the input sequence will be given no
	 * special meaning.
	 *
	 * @param input
	 *            The string to be literalized
	 * @return A literal string replacement
	 */
	public static String quote(String input) {
		return RegExp_Jso.quote(input);
	}

	private static boolean isScript() {
		if (isScript == null) {
			// cache
			isScript = GWT.isScript();
		}
		return isScript;
	}

	static RegExp construct(IRegExp impl) {
		return new RegExp(impl);
	}

	private IRegExp impl;

	public RegExp(IRegExp impl) {
		this.impl = impl;
	}

	public final MatchResult exec(String input) {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).exec(input);
		}
		return this.impl.exec(input);
	}

	public final boolean getGlobal() {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).getGlobal();
		}
		return this.impl.getGlobal();
	}

	public final boolean getIgnoreCase() {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).getIgnoreCase();
		}
		return this.impl.getIgnoreCase();
	}

	public final int getLastIndex() {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).getLastIndex();
		}
		return this.impl.getLastIndex();
	}

	public final boolean getMultiline() {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).getMultiline();
		}
		return this.impl.getMultiline();
	}

	public final String getSource() {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).getSource();
		}
		return this.impl.getSource();
	}

	public final String replace(String input, String replacement) {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).replace(input, replacement);
		}
		return this.impl.replace(input, replacement);
	}

	public final void setLastIndex(int lastIndex) {
		if (impl instanceof RegExp_Jvm) {
			((RegExp_Jvm) impl).setLastIndex(lastIndex);
			return;
		}
		this.impl.setLastIndex(lastIndex);
	}

	public final SplitResult split(String input) {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).split(input);
		}
		return impl.split(input);
	}

	public final SplitResult split(String input, int limit) {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).split(input, limit);
		}
		return this.impl.split(input, limit);
	}

	public final boolean test(String input) {
		if (impl instanceof RegExp_Jvm) {
			return ((RegExp_Jvm) impl).test(input);
		}
		return this.impl.test(input);
	}
}
