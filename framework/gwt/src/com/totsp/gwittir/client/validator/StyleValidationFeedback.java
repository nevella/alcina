/*
 * StyleValidationFeedback.java
 *
 * Created on July 26, 2007, 12:38 PM
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

import com.google.gwt.user.client.ui.UIObject;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class StyleValidationFeedback extends AbstractValidationFeedback {
	private String styleName;

	private HashMap previousStyles = new HashMap();

	/** Creates a new instance of StyleValidationFeedback */
	public StyleValidationFeedback(String styleName) {
		this.styleName = styleName;
	}

	@Override
	public void handleException(Object source, ValidationException exception) {
		UIObject object = (UIObject) source;
		if (this.styleName.equals(object.getStyleName())) {
			return;
		}
		String previousStyle = object.getStyleName() == null
				|| object.getStyleName().length() == 0 ? "default"
						: object.getStyleName();
		if (!this.previousStyles.containsKey(source))
			this.previousStyles.put(source, previousStyle);
		object.addStyleName(this.styleName);
	}

	@Override
	public void resolve(Object source) {
		UIObject object = (UIObject) source;
		String previousStyle = (String) this.previousStyles.get(source);
		if (previousStyle != null) {
			// GWT.log( "Reverting to style:" + previousStyle, null );
			object.setStyleName(previousStyle);
			// GWT.log( object.toString(), null );
			this.previousStyles.remove(source);
		}
	}
}
