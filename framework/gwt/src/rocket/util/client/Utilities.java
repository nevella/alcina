/*
 * Copyright Miroslav Pokorny
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
package rocket.util.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rocket.text.client.IndexedPlaceHolderReplacer;
import rocket.text.client.NamedPlaceHolderReplacer;

/**
 * The Checker class is a compilation of methods that check or assert that a
 * value satisfies a particular constraint.
 * 
 * It is useful for checking incoming parameters, verifying state etc.
 * 
 * @author Miroslav Pokorny (mP)
 */
@SuppressWarnings("deprecation")
public class Utilities {
	/**
	 * Convenient method which replaces all nbsp with a regular space.
	 * 
	 * @param text
	 * @return
	 */
	public static String changeNonBreakingSpacesToSpaces(final String text) {
		Checker.notEmpty("parameter:text", text);
		return text.replaceAll("&nbsp;", " ");
	}

	/**
	 * Convenient method which replaces all nbsp with a regular space.
	 * 
	 * @param text
	 * @return
	 */
	public static String changeSpacesToNonBreakingSpaces(final String text) {
		Checker.notEmpty("parameter:text", text);
		return text.replaceAll(" ", "&nbsp;");
	}

	/**
	 * Return the default java.lang.Object.toString() for the given object.
	 * 
	 * @param object
	 *            The object to format
	 * @return String the default format representation of the given object.
	 *         className - the at sign - the Objects hashcode ( in hex form
	 *         without the leading '0x' ) java.lang.Object@123def
	 */
	public static String defaultToString(final Object object) {
		return object == null ?
		/* handle null */
				String.valueOf(object) :
				/* class name including the leading package name */
				object.getClass().getName() + '@' +
				/* hashcode */
						Integer.toHexString(System.identityHashCode(object));
	} // defaultToString

	/**
	 * Calls the destroy method on the given object if it is destroyable.
	 * 
	 * @param object
	 *            A potentially destroyable object.
	 */
	public static void destroyIfNecessary(final Object object) {
		if (object instanceof Destroyable) {
			final Destroyable destroyable = (Destroyable) object;
			destroyable.destroy();
		}
	}

	/**
	 * Tests if the first string ends with the second ignoring case. This is
	 * equalivalent to String.endsWith() but ignores case.
	 * 
	 * @param first
	 *            The first string
	 * @param second
	 *            The second string
	 * @return True if the first string ends with the second.
	 */
	public static boolean endsWithIgnoringCase(final String first,
			final String second) {
		Checker.notNull("parameter:first", first);
		Checker.notNull("parameter:second", second);
		boolean startsWith = false;
		final int firstLength = first.length();
		final int secondLength = second.length();
		if (secondLength <= firstLength) {
			startsWith = true;
			for (int i = 0; i < secondLength; i++) {
				final char c = first.charAt(firstLength - 1 - i);
				final char d = second.charAt(secondLength - 1 - i);
				if (Character.toLowerCase(c) != Character.toLowerCase(d)) {
					startsWith = false;
					break;
				}
			}
		}
		return startsWith;
	} // endsWithIgnoringCase

	/**
	 * Convenience method that escapes all newline, carriage returns and double
	 * quotes into their java string form.
	 * 
	 * @param string
	 * @return
	 */
	public static String escape(final String string) {
		return null == string ? null
				: string.replaceAll("\n", "\\n").replaceAll("\r", "\\\r")
						.replaceAll("\"", "\\\"");
	}

	/**
	 * Builds a new string substituting the placeholders within text with values
	 * from values. The placeholders found in the text are used as keys to the
	 * given map of values which will supply the replacements.
	 * 
	 * @param text
	 *            Some text that includes placeholders
	 * @param values
	 *            An map of values which will be used to replace placeholders
	 * @return The string after replacements.
	 */
	public static String format(final String text, final Map values) {
		final NamedPlaceHolderReplacer replacer = new NamedPlaceHolderReplacer();
		replacer.setValues(values);
		return replacer.execute(text);
	}

	/**
	 * Builds a new string substituting the placeholders within text with values
	 * from values. The placeholders found in the text are used as indexes to
	 * the given array of values which will supply the replacements.
	 * 
	 * <pre>
	 * String input = &quot;Apple's are ${0} and banana are ${1}.&quot;
	 * 
	 * String[] values = new String[] { &quot;green&quot;, &quot;yellow&quot; };
	 * 
	 * String output = Utilities.format(input, values); // =
	 * 													// &quot;Apple's are green
	 * 													// and bananas are
	 * 													// yellow.&quot;
	 * </pre>
	 * 
	 * @param text
	 *            Some text that includes placeholders
	 * @param values
	 *            An array of values which will be used to replace placeholders
	 * @return The string after replacements.
	 */
	public static String format(final String text, final Object[] values) {
		final IndexedPlaceHolderReplacer replacer = new IndexedPlaceHolderReplacer();
		replacer.setValues(values);
		return replacer.execute(text);
	}

	/**
	 * Accepts a encoded string and returns the original decoded value.
	 * 
	 * @param htmlEncodedText
	 * @return
	 */
	public static String htmlDecode(final String htmlEncodedText) {
		Checker.notNull("parameter:htmlEncodedText", htmlEncodedText);
		final StringBuffer buf = new StringBuffer();
		final int length = htmlEncodedText.length();
		for (int i = 0; i < length;) {
			final char c = htmlEncodedText.charAt(i);
			i++;
			if ('&' == c) {
				final int semiColon = htmlEncodedText.indexOf(';', i);
				final String entity = htmlEncodedText.substring(i, semiColon);
				i = semiColon + 1;
				if (entity.equals("lt")) {
					buf.append("<");
					continue;
				}
				if (entity.equals("gt")) {
					buf.append(">");
					continue;
				}
				if (entity.equals("amp")) {
					buf.append("&");
					continue;
				}
				if (entity.equals("quot")) {
					buf.append('"');
					continue;
				}
				throw new RuntimeException(
						"Unknown/unsupported html entity &" + entity + ";");
			}
			buf.append(c);
		}
		return buf.toString();
	}

	/**
	 * Accepts a plain string escaping various characters so that the given
	 * string is html encoded.
	 * 
	 * @param plainText
	 * @return
	 */
	public static String htmlEncode(final String plainText) {
		Checker.notNull("parameter:plainText", plainText);
		final StringBuffer buf = new StringBuffer();
		final int length = plainText.length();
		for (int i = 0; i < length; i++) {
			final char c = plainText.charAt(i);
			if ('<' == c) {
				buf.append("&lt;");
				continue;
			}
			if ('>' == c) {
				buf.append("&gt;");
				continue;
			}
			if ('&' == c) {
				buf.append("&amp;");
				continue;
			}
			// if ('\'' == c) {
			// buf.append("&apos;");
			// continue;
			// }
			if ('"' == c) {
				buf.append("&quot;");
				continue;
			}
			buf.append(c);
		}
		return buf.toString();
	}

	/**
	 * Scans the parameter:string for the parameter:test ignoring case when
	 * comparing characters.
	 * 
	 * @param string
	 * @param search
	 *            If test is empty -1 is always returned.
	 * @return -1 if the string was not found or the index of the first matching
	 *         character
	 */
	public static int indexOfIgnoringCase(final String string,
			final String search) {
		Checker.notNull("parameter:string", string);
		Checker.notNull("parameter:search", search);
		int index = -1;
		final int stringLength = string.length();
		final int testLength = search.length();
		if (stringLength > 1 || testLength > 1) {
			final char firstCharOfTest = Character
					.toLowerCase(search.charAt(0));
			final int lastStringCharacterToCheck = stringLength - testLength
					+ 1;
			for (int i = 0; i < lastStringCharacterToCheck; i++) {
				if (firstCharOfTest == Character
						.toLowerCase(string.charAt(i))) {
					index = i;
					for (int j = 1; j < testLength; j++) {
						final char c = string.charAt(i + j);
						final char otherChar = search.charAt(j);
						if (Character.toLowerCase(c) != Character
								.toLowerCase(otherChar)) {
							index = -1;
							break;
						}
					}
					if (-1 != index) {
						break;
					}
				}
			}
		}
		return index;
	} // indexOfIgnoringCase

	/**
	 * Joins a string array into one long string with each value separated by
	 * the separator parameter. If an element of array is null it is skipped
	 * (null) is not added to the built up string.
	 * 
	 * @param array
	 * @param separator
	 * @return
	 */
	public static String join(final String[] array, final String separator) {
		Checker.notNull("parameter:array", array);
		Checker.notNull("parameter:separator", separator);
		final StringBuffer buf = new StringBuffer();
		boolean addSeparator = false;
		for (int i = 0; i < array.length; i++) {
			final String element = array[i];
			if (null == element) {
				continue;
			}
			if (addSeparator) {
				buf.append(separator);
			}
			buf.append(element);
			addSeparator = true;
		}
		return buf.toString();
	}

	/**
	 * If the input parameter is null return empty string, all other values are
	 * returned verbatim.
	 * 
	 * @param input
	 * @return
	 */
	public static String nullToEmpty(final String input) {
		return input == null ? "" : input;
	}

	/**
	 * Invokes {@link #padLeft String, int, char }with a default space pad
	 * character.
	 * 
	 * @param text
	 * @param length
	 * @return
	 */
	public static String padLeft(final String text, final int length) {
		return padLeft(text, length, ' ');
	}

	/**
	 * Pads the rightmost characters of text with $pad so that its length
	 * matches that of the parameter:length.
	 * 
	 * @param text
	 * @param length
	 * @param pad
	 * @return
	 */
	public static String padLeft(final String text, final int length,
			final char pad) {
		Checker.notNull("parameter:text", text);
		final int textLength = text.length();
		final int requiredPadding = length - textLength;
		if (requiredPadding < 0) {
			Checker.fail("parameter:text",
					"THe parameter:text is longer than the given lineLength which is used to determine the required padding, text\""
							+ text + "\", text.length: " + textLength
							+ ", length: " + length);
		}
		final StringBuffer buf = new StringBuffer(textLength + requiredPadding);
		for (int i = 0; i < requiredPadding; i++) {
			buf.append(pad);
		}
		buf.append(text);
		return buf.toString();
	}

	/**
	 * Invokes {@link #padRight(String, int, char )}with a default space pad
	 * character.
	 * 
	 * @param text
	 * @param length
	 * @return
	 */
	public static String padRight(final String text, final int length) {
		return padRight(text, length, ' ');
	}

	/**
	 * Pads the rightmost characters of text with $pad so that its length
	 * matches that of the parameter:length.
	 * 
	 * @param text
	 * @param length
	 * @param pad
	 * @return
	 */
	public static String padRight(final String text, final int length,
			final char pad) {
		Checker.notNull("parameter:text", text);
		final int textLength = text.length();
		final int requiredPadding = length - textLength;
		if (requiredPadding < 0) {
			Checker.fail("parameter:text",
					"THe parameter:text is longer than the given lineLength which is used to determine the required padding, text\""
							+ text + "\", text.length: " + textLength
							+ ", length: " + length);
		}
		final StringBuffer buf = new StringBuffer(textLength + requiredPadding);
		buf.append(text);
		for (int i = 0; i < requiredPadding; i++) {
			buf.append(pad);
		}
		return buf.toString();
	}

	/**
	 * Convenience method which quotes non null strings as well as escaping
	 * newlines, carriage returns and double quotes.
	 * 
	 * @param string
	 * @return
	 */
	public static String quotedEscape(final String string) {
		return null == string ? null : "\"" + escape(string) + "\"";
	}

	/**
	 * Performs a similar to task to the J2SE java.lang.StringTokenizer class
	 * splitting a larger string into many smaller ones. An ideal example is
	 * splitting up a comma delimited string "apple,banana,carrot" becomes three
	 * strings "apple", "banana", "carrot"
	 * 
	 * @param input
	 *            The initial string
	 * @param delimiter
	 *            A string containing delimiter characters.
	 * @param ignoreDelimiters
	 *            A flag indicating whether delimiters should be included in the
	 *            returned Strings
	 * @return An array of the tokens found
	 */
	public static String[] split(final String input, final String delimiter,
			final boolean ignoreDelimiters) {
		Checker.notNull("parameter:input", input);
		Checker.notEmpty("parameter:delimiter", delimiter);
		final List<String> tokens = new ArrayList<String>();
		final int stringLength = input.length();
		if (stringLength > 0) {
			final char[] chars = input.toCharArray();
			int firstChar = 0;
			for (int i = 0; i < stringLength; i++) {
				final char c = chars[i];
				final int isTokenTest = delimiter.indexOf(c);
				/* token found! */
				if (isTokenTest != -1) {
					tokens.add(input.substring(firstChar, i));
					/* include delimiter in the output ??? */
					if (false == ignoreDelimiters) {
						tokens.add(String.valueOf(c));
					}
					/* mark the beginning of the next token... */
					firstChar = i + 1;
					continue;
				}
			} // for each char
			/* the last token will not be terminated.. add */
			tokens.add(input.substring(firstChar));
		} // if
		/* copy the splitted strings into a String array */
		final String[] array = new String[tokens.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = tokens.get(i);
		}
		return array;
	} // split

	/**
	 * Tests if the first string starts with the second ignoring case. This is
	 * equalivalent to String.startsWith() but ignores case.
	 * 
	 * @param first
	 *            The first string
	 * @param second
	 *            The second string
	 * @return True if the first string starts with the second.
	 */
	public static boolean startsWithIgnoringCase(final String first,
			final String second) {
		Checker.notNull("parameter:first", first);
		Checker.notNull("parameter:second", second);
		boolean startsWith = false;
		final int secondLength = second.length();
		if (secondLength <= first.length()) {
			startsWith = true;
			for (int i = 0; i < secondLength; i++) {
				final char c = first.charAt(i);
				final char d = second.charAt(i);
				if (Character.toLowerCase(c) != Character.toLowerCase(d)) {
					startsWith = false;
					break;
				}
			}
		}
		return startsWith;
	} // startsWithIgnoringCase

	/**
	 * Converts a cssPropertyName into a javascript propertyName. eg
	 * 
	 * <pre>
	 * String css = &quot;background-color&quot;
	 * String js = toCamelCase(css);
	 * System.out.println(css + &quot;&gt;&quot; + js); // prints [[[background-color &gt;
	 * 									// backgroundColor.]]] without the
	 * 									// brackets.
	 * </pre>
	 * 
	 * @param cssPropertyName
	 * @return
	 */
	public static String toCamelCase(final String cssPropertyName) {
		Checker.notEmpty("parameter:cssPropertyName", cssPropertyName);
		String propertyName = cssPropertyName;
		int i = 0;
		while (true) {
			final int nextDash = propertyName.indexOf('-', i);
			if (-1 == nextDash) {
				break;
			}
			final char charAfterDash = propertyName.charAt(nextDash + 1);
			propertyName = propertyName.substring(0, nextDash)
					+ Character.toUpperCase(charAfterDash)
					+ propertyName.substring(nextDash + 2);
			i = nextDash + 2;
		}
		return propertyName;
	}

	/**
	 * Takes a javascript styled propertyName and converts it into a css styled
	 * propertyName. eg backgroundColor becomes background-color
	 * 
	 * @param propertyName
	 * @return
	 */
	public static String toCssPropertyName(final String propertyName) {
		Checker.notEmpty("parameter:propertyName", propertyName);
		return toCssPropertyName0(propertyName);
	}

	/**
	 * Useful method that only trims whitespace from the left or beginning of
	 * the given string
	 * 
	 * @param string
	 * @return
	 */
	public static String trimLeft(final String string) {
		Checker.notNull("parameter:string", string);
		String trimmed = "";
		final int length = string.length();
		int i = 0;
		while (i < length) {
			final char c = string.charAt(i);
			if (Character.isSpace(c)) {
				i++;
				continue;
			}
			trimmed = string.substring(i);
			break;
		}
		return trimmed;
	}

	/**
	 * Useful method that only trims whitespace from the right or end of the
	 * given string
	 * 
	 * @param string
	 * @return
	 */
	public static String trimRight(final String string) {
		Checker.notNull("parameter:string", string);
		String trimmed = "";
		final int length = string.length();
		int i = length;
		while (i > 0) {
			i--;
			final char c = string.charAt(i);
			if (Character.isSpace(c)) {
				continue;
			}
			trimmed = string.substring(0, i + 1);
			break;
		}
		return trimmed;
	}

	/**
	 * Takes a url encoded string and returns the decoded form.
	 * 
	 * @param encoded
	 * @return
	 */
	public static String urlDecode(final String encoded) {
		Checker.notNull("parameter:encoded", encoded);
		final StringBuffer decoded = new StringBuffer();
		int i = 0;
		while (i < encoded.length()) {
			final char c = encoded.charAt(i);
			i++;
			if ('+' == c) {
				decoded.append(' ');
				continue;
			}
			if ('%' != c) {
				decoded.append(c);
				continue;
			}
			final int hi = Character.digit(encoded.charAt(i), 16);
			i++;
			final int lo = Character.digit(encoded.charAt(i), 16);
			i++;
			decoded.append((char) (hi * 16 + lo));
		}
		return decoded.toString();
	}

	native private static String
			toCssPropertyName0(final String propertyName)/*-{
															return propertyName.replace(/([A-Z])/g, "-$1" ).toLowerCase()
															}-*/;
}
