package cc.alcina.framework.common.client.util;

public class FormatBuilder {
	private StringBuilder sb = new StringBuilder();

	private String separator = "";

	public FormatBuilder separator(String separator) {
		this.separator = separator;
		return this;
	}

	public FormatBuilder line(String template, Object... args) {
		return format(template, args).newLine();
	}

	public FormatBuilder format(String template, Object... args) {
		maybeAppendSeparator();
		sb.append(CommonUtils.formatJ(template, args));
		return this;
	}

	private void maybeAppendSeparator() {
		if (sb.length() > 0 && separator.length() > 0) {
			sb.append(separator);
		}
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	public FormatBuilder newLine() {
		sb.append("\n");
		return this;
	}

	public void appendIfNonEmpty(String optional) {
		if (sb.length() > 0) {
			sb.append(optional);
		}
	}
}