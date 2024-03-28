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
package cc.alcina.framework.common.client.gwittir.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class RegexValidator
		implements ParameterisedValidator, RequiresSourceValidator {
	public static final String PARAM_REGEX = "regex";

	public static final String PARAM_FEEDBACK_MESSAGE = "feedback-message";

	public static final String REGEX_REPLACE = "234IBBDA";

	private String regex;

	private String feedbackMessage;

	public RegexValidator() {
	}

	public RegexValidator(String regex) {
		this.regex = regex;
	}

	public RegexValidator feedbackMessage(String feedbackMessage) {
		this.feedbackMessage = feedbackMessage;
		return this;
	}

	public String getRegex() {
		return this.regex;
	}

	@Override
	public void setParameters(NamedParameter[] params) {
		if (regex == null) {
			regex = NamedParameter.Support.stringValue(params, PARAM_REGEX,
					null);
			feedbackMessage = NamedParameter.Support.stringValue(params,
					PARAM_FEEDBACK_MESSAGE, null);
		}
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			return null;
		}
		value = value.toString().trim();
		String sz = value.toString();
		if (!sz.replaceAll(getRegex(), REGEX_REPLACE).equals(REGEX_REPLACE)) {
			String message = feedbackMessage != null ? feedbackMessage
					: Ax.format("Does not match regex ('%s')", getRegex());
			throw new ValidationException(message, RegexValidator.class);
		}
		return value;
	}

	@Override
	public void setSourceObject(Entity sourceObject) {
		// NOOP
	}

	public void onProperty(Property property) {
		Optional.ofNullable(property.annotation(Regex.class)).map(Regex::value)
				.ifPresent(this::setRegex);
		Optional.ofNullable(property.annotation(FeedbackMessage.class))
				.map(FeedbackMessage::value).ifPresent(this::feedbackMessage);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Regex {
		String value();
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface FeedbackMessage {
		String value();
	}
}