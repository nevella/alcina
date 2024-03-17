/*
 * ValidationFeedback.java
 *
 * Created on July 16, 2007, 12:58 PM
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
package com.totsp.gwittir.client.validator;

/**
 * TODO this needs a .resolve() method
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public interface ValidationFeedback {
	public void handleException(Object source, ValidationException exception);

	public void resolve(Object source);

	default void resolve(Object source, boolean beforeException) {
		resolve(source);
	}

	public static interface Provider {
		public abstract Builder builder();

		public abstract class Builder {
			protected Direction direction;

			protected String propertyName;

			public abstract ValidationFeedback createFeedback();

			public Builder displayDirection(Direction direction) {
				this.direction = direction;
				return this;
			}

			public Builder forPropertyName(String propertyName) {
				this.propertyName = propertyName;
				return this;
			}
		}

		public enum Direction {
			TOP, RIGHT, BOTTOM, LEFT
		}
	}

	public static class Support {
		public static Provider DEFAULT_PROVIDER;
	}
}
