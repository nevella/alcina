package cc.alcina.framework.gwt.client.gwittir.widget;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.gwittir.widget.FileData;

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
