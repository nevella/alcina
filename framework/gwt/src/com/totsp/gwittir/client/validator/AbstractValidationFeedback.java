/*
 * AbstractValidationFeedback.java
 *
 * Created on July 16, 2007, 7:43 PM
 *
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

import java.util.HashMap;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public abstract class AbstractValidationFeedback implements ValidationFeedback {
	private HashMap<Class, String> mappings = new HashMap<Class, String>();

	public AbstractValidationFeedback() {
		super();
	}

	public AbstractValidationFeedback addMessage(Class validatorClass,
			String message) {
		mappings.put(validatorClass, message);
		return this;
	}

	public HashMap<Class, String> getMappings() {
		return this.mappings;
	}

	protected String getMessage(ValidationException validationException) {
		Class clazz = validationException.getValidatorClass();
		String message = null;
		if (validationException.getValidatorClass() != null) {
			message = mappings.get(clazz);
		}
		return (message == null) ? validationException.getMessage() : message;
	}
}
