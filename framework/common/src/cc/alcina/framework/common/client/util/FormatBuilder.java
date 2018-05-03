package cc.alcina.framework.common.client.util;

public class FormatBuilder {
	private StringBuilder sb = new StringBuilder();

	private String separator = "";

	public void appendIfBuilderNonEmpty(String optional) {
		if (sb.length() > 0) {
			sb.append(optional);
		}
	}

	public void appendIfNotBlank(String optional) {
		if (Ax.notBlank(optional)) {
			maybeAppendSeparator();
			sb.append(optional);
		}
	}

	public void appendIfNotBlank(Object... optionals) {
		for (Object optional : optionals) {
			if (optional == null) {
			} else {
				appendIfNotBlank(optional.toString());
			}
		}
	}

	public FormatBuilder format(String template, Object... args) {
		maybeAppendSeparator();
		sb.append(CommonUtils.formatJ(template, args));
		return this;
	}

	public FormatBuilder line(String template, Object... args) {
		return format(template, args).newLine();
	}

	public FormatBuilder newLine() {
		sb.append("\n");
		return this;
	}

	public FormatBuilder separator(String separator) {
		this.separator = separator;
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	private void maybeAppendSeparator() {
		if (sb.length() > 0 && separator.length() > 0) {
			sb.append(separator);
		}
	}
}