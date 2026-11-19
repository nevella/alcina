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
package rocket.text.client;

import rocket.util.client.Checker;

/**
 * Abstract class that helps with scanning and replacing placeholders with
 * actual values.
 * 
 * @author Miroslav Pokorny
 */
abstract public class PlaceHolderReplacer {
	/**
	 * Travels over the input string replacing placeholders with values
	 * returning the built result.
	 * 
	 * @param text
	 * @return
	 */
	protected String execute(final String text) {
		Checker.notNull("parameter:text", text);
		final StringBuffer buf = new StringBuffer();
		int i = 0;
		final int messageLength = text.length();
		while (i < messageLength) {
			// find escape character...
			final int escapeIndex = text.indexOf('\\', i);
			if (-1 != escapeIndex) {
				final int characterAfterIndex = escapeIndex + 1;
				if (escapeIndex == messageLength) {
					Checker.fail(
							"Broken message, trailing escape character found.");
				}
				buf.append(text.substring(i, escapeIndex));
				final char characterAfter = text.charAt(characterAfterIndex);
				if ('$' == characterAfter || '\\' == characterAfter) {
					buf.append(characterAfter);
					i = characterAfterIndex + 1;
					continue;
				}
				Checker.fail(
						"Invalid escape character found in format string \""
								+ text + "\" at " + characterAfterIndex);
			}
			// find the start placeholder
			final int placeHolderStartIndex = text.indexOf("${", i);
			if (-1 == placeHolderStartIndex) {
				buf.append(text.substring(i, messageLength));
				break;
			}
			buf.append(text.substring(i, placeHolderStartIndex));
			// find the end placeholder
			final int placeHolderEndIndex = text.indexOf('}',
					placeHolderStartIndex + 2);
			if (-1 == placeHolderEndIndex) {
				Checker.fail(
						"Unable to find placeholder end after finding start, \""
								+ text.substring(i, messageLength - i) + "\".");
			}
			// extract the index in between...
			final String placeHolder = text.substring(2 + placeHolderStartIndex,
					placeHolderEndIndex);
			buf.append(this.getValue(placeHolder));
			// advance past placeholder
			i = placeHolderEndIndex + 1;
		}
		return buf.toString();
	}

	/**
	 * Sub-classes must resolve the placeholder to a value.
	 * 
	 * @param placeholder
	 * @return
	 */
	abstract protected String getValue(String placeholder);
}
