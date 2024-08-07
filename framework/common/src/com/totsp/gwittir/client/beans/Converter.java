/*
 * Converter.java
 *
 * Created on July 16, 2007, 12:54 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.totsp.gwittir.client.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 *
 */
public interface Converter<T, C> extends Function<T, C> {
	public static final Converter<Object, String> TO_STRING_CONVERTER = new Converter<Object, String>() {
		@Override
		public String convert(Object original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Converter<Collection, Object> FROM_COLLECTION_CONVERTER = new Converter<Collection, Object>() {
		@Override
		public Object convert(Collection original) {
			if ((original == null) || (original.size() == 0)) {
				return null;
			}
			return original.iterator().next();
		}
	};

	public static final Converter<Object, Collection> TO_COLLECTION_CONVERTER = new Converter<Object, Collection>() {
		@Override
		public Collection convert(Object original) {
			ArrayList ret = new ArrayList();
			ret.add(original);
			return ret;
		}
	};

	public static final Converter<Integer, String> INTEGER_TO_STRING_CONVERTER = new Converter<Integer, String>() {
		@Override
		public String convert(Integer original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Converter<String, Integer> STRING_TO_INTEGER_CONVERTER = new Converter<String, Integer>() {
		@Override
		public Integer convert(String original) {
			return (original == null) ? null : Integer.valueOf(original);
		}
	};

	public static final Converter<String, Double> STRING_TO_DOUBLE_CONVERTER = new Converter<String, Double>() {
		@Override
		public Double convert(String original) {
			return (original == null) ? null : Double.valueOf(original);
		}
	};

	public static final Converter<Long, String> LONG_TO_STRING_CONVERTER = new Converter<Long, String>() {
		@Override
		public String convert(Long original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Converter<Boolean, String> BOOLEAN_TO_STRING_CONVERTER = new Converter<Boolean, String>() {
		@Override
		public String convert(Boolean original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Converter<Double, String> DOUBLE_TO_STRING_CONVERTER = new Converter<Double, String>() {
		@Override
		public String convert(Double original) {
			return (original == null) ? null : original.toString();
		}
	};

	public static final Converter<String, Long> STRING_TO_LONG_CONVERTER = new Converter<String, Long>() {
		@Override
		public Long convert(String original) {
			return (original == null) ? null : Long.valueOf(original);
		}
	};

	public static final Converter<String, Boolean> STRING_TO_BOOLEAN_CONVERTER = new Converter<String, Boolean>() {
		@Override
		public Boolean convert(String original) {
			return (original == null) ? null : Boolean.valueOf(original);
		}
	};

	static <T, C> Converter<T, C> nullToNull(Function<T, C> function) {
		return new Converter<T, C>() {
			@Override
			public C convert(T original) {
				if (original == null) {
					return null;
				}
				return function.apply(original);
			}
		};
	}

	static <T, C> Converter<T, C> wrap(Function<T, C> function) {
		return new Converter<T, C>() {
			@Override
			public C convert(T original) {
				return function.apply(original);
			}
		};
	}

	@Override
	default C apply(T t) {
		return convert(t);
	}

	C convert(T original);
}
