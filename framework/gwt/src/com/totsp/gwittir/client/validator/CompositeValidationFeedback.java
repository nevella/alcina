/*
 * CompositeValidationFeedback.java
 *
 * Created on July 26, 2007, 12:43 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.client.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class CompositeValidationFeedback extends AbstractValidationFeedback {
	private ArrayList<ValidationFeedback> feedbacks = new ArrayList<ValidationFeedback>();

	/** Creates a new instance of CompositeValidationFeedback */
	public CompositeValidationFeedback() {
		super();
	}

	public CompositeValidationFeedback(ValidationFeedback... feedback) {
		super();
		addAll(feedback);
		this.feedbacks.addAll(Arrays.asList(feedback));
	}

	private void addAll(ValidationFeedback[] feedbackToAdd) {
		this.feedbacks.addAll(Arrays.asList(feedbackToAdd));
		for (ValidationFeedback validationFeedback : feedbackToAdd) {
			if (validationFeedback instanceof AbstractValidationFeedback) {
				getMappings().putAll(
						((AbstractValidationFeedback) validationFeedback)
								.getMappings());
			}
		}
		pushMappings();
	}

	protected void pushMappings() {
		for (ValidationFeedback validationFeedback : feedbacks) {
			if (validationFeedback instanceof AbstractValidationFeedback) {
				Set<Entry<Class, String>> entrySet = getMappings().entrySet();
				for (Entry<Class, String> entry : entrySet) {
					((AbstractValidationFeedback) validationFeedback)
							.addMessage(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	@Override
	public AbstractValidationFeedback addMessage(
			Class validationExceptionClass, String message) {
		getMappings().put(validationExceptionClass, message);
		pushMappings();
		return this;
	}

	public void handleException(Object source, ValidationException exception) {
		for (Iterator it = feedbacks.iterator(); it.hasNext();) {
			((ValidationFeedback) it.next()).handleException(source, exception);
		}
	}

	public void resolve(Object source) {
		for (Iterator it = feedbacks.iterator(); it.hasNext();) {
			((ValidationFeedback) it.next()).resolve(source);
		}
	}

	public CompositeValidationFeedback add(ValidationFeedback feedback) {
		addAll(new ValidationFeedback[] { feedback });
		return this;
	}
}
