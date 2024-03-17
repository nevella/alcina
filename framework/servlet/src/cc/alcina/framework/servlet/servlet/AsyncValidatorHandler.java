package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;

public interface AsyncValidatorHandler {
	ValidationResult validate(String input);
}
