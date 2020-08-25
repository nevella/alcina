/*
 * StyleValidationFeedback.java
 *
 * Created on July 26, 2007, 12:38 PM
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
package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 *
 */
public class LuxStyleValidationFeedback extends AbstractValidationFeedback {
	private String styleName;

	/** Creates a new instance of StyleValidationFeedback */
	public LuxStyleValidationFeedback(String styleName) {
		this.styleName = styleName;
	}

	@Override
	public void handleException(Object source, ValidationException exception) {
		Widget object = (Widget) source;
		object.addStyleName(this.styleName);
		Widget formElement = WidgetUtils.getAncestorWidgetSatisfyingTypedCallback(
				object,
				widget -> LuxFormStyle.LUX_FORM_ELEMENT.hasStyle(widget));
		formElement.addStyleName(this.styleName);
	}

	@Override
	public void resolve(Object source) {
		Widget object = (Widget) source;
		object.removeStyleName(this.styleName);
		Widget formElement = WidgetUtils.getAncestorWidgetSatisfyingTypedCallback(
				object,
				widget -> LuxFormStyle.LUX_FORM_ELEMENT.hasStyle(widget));
		formElement.removeStyleName(this.styleName);
	}
}
