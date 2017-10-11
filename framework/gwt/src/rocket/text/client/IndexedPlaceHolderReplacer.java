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
 * This placeholder scans for placeholders and uses the placeholder as an index
 * into a String array.
 * 
 * @author Miroslav Pokorny
 */
public class IndexedPlaceHolderReplacer extends PlaceHolderReplacer {
	@Override
	public String execute(final String text) {
		return super.execute(text);
	}

	@Override
	protected String getValue(final String placeHolder) {
		try {
			final int index = Integer.parseInt(placeHolder);
			return String.valueOf(this.getValues()[index]);
		} catch (final NumberFormatException badIndex) {
			Checker.fail("Placeholder index does not contain a number \""
					+ placeHolder + "\".");
			return null;// unreachable
		}
	}

	/**
	 * A String array that contains the values for each of the placeholders
	 */
	private Object[] values;

	protected Object[] getValues() {
		Checker.notNull("field:values", values);
		return this.values;
	}

	public void setValues(final Object[] values) {
		Checker.notNull("parameter:values", values);
		this.values = values;
	}
}
