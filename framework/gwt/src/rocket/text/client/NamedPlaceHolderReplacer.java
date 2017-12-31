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

import java.util.Map;

import rocket.util.client.Checker;

/**
 * This placeholder uses the placeholder as a key into a map to fetch the value.
 * 
 * @author Miroslav Pokorny
 */
public class NamedPlaceHolderReplacer<V> extends PlaceHolderReplacer {
	private Map<String, V> values;

	@Override
	public String execute(final String text) {
		return super.execute(text);
	}

	public void setValues(final Map<String, V> values) {
		Checker.notNull("parameter:values", values);
		this.values = values;
	}

	@Override
	protected String getValue(final String placeHolder) {
		final String value = (String) this.getValues().get(placeHolder);
		if (null == value) {
			Checker.fail("Unable to find placeholder \"" + placeHolder + "\".");
		}
		return value;
	}

	protected Map<String, V> getValues() {
		Checker.notNull("field:values", values);
		return this.values;
	}
}
