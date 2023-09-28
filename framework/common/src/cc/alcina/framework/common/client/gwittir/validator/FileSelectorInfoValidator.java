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

import java.util.Arrays;
import java.util.List;

import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.widget.FileData;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class FileSelectorInfoValidator implements ParameterisedValidator {
	/*
	 * comma-separated list: csv, xml, pdf
	 */
	public static final String PARAM_EXTENSIONS = "extensions";

	public static final String PARAM_REQUIRED = "required";

	private String extensions;

	private boolean required;

	public FileSelectorInfoValidator() {
	}

	@Override
	public void setParameters(NamedParameter[] params) {
		NamedParameter p = NamedParameter.Support.getParameter(params,
				PARAM_EXTENSIONS);
		extensions = p.stringValue();
		p = NamedParameter.Support.getParameter(params, PARAM_REQUIRED);
		required = p.booleanValue();
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			if (required) {
				throw new ValidationException("Required field");
			} else {
				return null;
			}
		}
		FileData info = (FileData) value;
		if (info == null || info.getFileName() == null
				|| info.getBytes() == null) {
			if (required) {
				throw new ValidationException("Required field");
			}
		} else {
			if (Ax.notBlank(extensions)) {
				if (!info.getFileName().contains(".")) {
					throwInvalidExtension();
				}
				String fileExtension = info.getFileName()
						.replaceFirst(".+(?:\\.(.+))", "$1");
				List<String> parts = Arrays.asList(extensions.split(", ?"));
				if (!parts.contains(fileExtension)) {
					throwInvalidExtension();
				}
			}
		}
		return value;
	}

	private void throwInvalidExtension() throws ValidationException {
		throw new ValidationException(
				Ax.format("Invalid file extension - valid extensions are: %s",
						extensions));
	}
}