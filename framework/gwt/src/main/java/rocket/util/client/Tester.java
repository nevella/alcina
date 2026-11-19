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

/**
 * The Tester class is a compilation of useful test methods, ie most of them
 * return booleans.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class Tester {
	static public boolean equals(final double value, final double otherValue,
			final double epsilon) {
		return (value - epsilon <= otherValue)
				&& (value + epsilon >= otherValue);
	}

	static public boolean equals(final long value, final long otherValue,
			final long epsilon) {
		return (value - epsilon <= otherValue)
				&& (value + epsilon >= otherValue);
	}

	public static boolean isGet(final String method) {
		return Constants.GET.equals(method);
	}

	public static boolean isHttp(final String protocol) {
		return Constants.HTTP.equals(protocol);
	}

	public static boolean isHttps(final String protocol) {
		return Constants.HTTPS.equals(protocol);
	}

	public static boolean isNullOrEmpty(final String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isPost(final String method) {
		return Constants.POST.equals(method);
	}

	public static boolean nullSafeEquals(final Object first,
			final Object second) {
		boolean result = false;
		while (true) {
			if (null == first && null == second) {
				result = true;
				break;
			}
			if (null == first && null != second) {
				result = false;
				break;
			}
			if (null != first && null == second) {
				result = false;
				break;
			}
			result = first.equals(second);
			break;
		}
		return result;
	}

	public static boolean nullSafeIdentity(final Object first,
			final Object second) {
		return null == first && null == second ? true : first == second;
	}
}
