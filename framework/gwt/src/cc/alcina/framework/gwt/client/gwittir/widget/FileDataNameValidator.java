package cc.alcina.framework.gwt.client.gwittir.widget;

import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.gwittir.widget.FileData;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.ValidationException;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.Validator;

@Reflected
public abstract class FileDataNameValidator implements Validator {
	String filenamePattern;

	String invalidNameMessage;

	protected FileDataNameValidator(String filenamePattern,
			String invalidNameMessage) {
		this.filenamePattern = filenamePattern;
		this.invalidNameMessage = invalidNameMessage;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		FileData fileData = (FileData) value;
		if (fileData != null
				&& !fileData.getFileName().matches(filenamePattern)) {
			throw new ValidationException(getInvalidMessage());
		}
		return value;
	}

	protected String getInvalidMessage() {
		return invalidNameMessage;
	}

	public static class Docx extends FileDataNameValidator {
		public Docx() {
			super(".+\\.docx", "The file must be of type .docx");
		}
	}
}
