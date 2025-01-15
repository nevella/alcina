/*
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
package cc.alcina.framework.common.client.util;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 *
 * @author Nick Reddel
 */
public interface ToStringFunction<T> extends Function<T, String> {
	public interface Bidi<T>
			extends BidiFunction<T, String>, ToStringFunction<T> {
	}

	@Reflected
	public static class ClassName implements ToStringFunction<Class> {
		@Override
		public String apply(Class t) {
			return t == null ? null : t.getName();
		}
	}

	/**
	 * Called this because ToStringFunction.Indentity is the default value (and
	 * thus ignored)
	 *
	 *
	 *
	 */
	@Reflected
	public static class ExplicitIdentity implements ToStringFunction<String> {
		@Override
		public String apply(String t) {
			return t;
		}
	}

	@Reflected
	public static class Identity implements ToStringFunction<String> {
		@Override
		public String apply(String t) {
			return t;
		}
	}

	@Reflected
	public static class ToCssName implements ToStringFunction<String> {
		@Override
		public String apply(String t) {
			return Ax.cssify(t);
		}
	}

	@Reflected
	public static class Value implements ToStringFunction<Object> {
		@Override
		public String apply(Object t) {
			return t == null ? null : t.toString();
		}
	}

	@Reflected
	public static class Friendly implements ToStringFunction<Object> {
		@Override
		public String apply(Object t) {
			return Ax.friendly(t);
		}
	}

	@Reflected
	public static class FriendlyLower implements ToStringFunction<Object> {
		@Override
		public String apply(Object t) {
			return Ax.friendly(t).toLowerCase();
		}
	}

	@Reflected
	public static class Existence implements ToStringFunction<Boolean> {
		@Override
		public String apply(Boolean t) {
			return t == null || !t ? null : Boolean.TRUE.toString();
		}
	}
}