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

import java.util.Comparator;

/**
 * Unfortunately the GWT.String class does not include a String comparator. This
 * class provides both a normal exact case comparator as well as a case ignorant
 * one.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class StringComparator implements Comparator {
	public final static StringComparator COMPARATOR = new StringComparator(
			false);

	public final static StringComparator IGNORE_CASE_COMPARATOR = new StringComparator(
			true);

	private boolean ignoreCase;

	protected StringComparator(final boolean ignoreCase) {
		super();
		this.setIgnoreCase(ignoreCase);
	}

	public int compare(final Object object, final Object otherObject) {
		return this.compare((String) object, (String) otherObject);
	}

	public int compare(final String string, final String otherString) {
		return this.isIgnoreCase()
				? string.toLowerCase().compareTo(otherString.toLowerCase())
				: string.compareTo(otherString);
	}

	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	public void setIgnoreCase(final boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public String toString() {
		return super.toString() + ", ignoreCase: " + ignoreCase;
	}
}
