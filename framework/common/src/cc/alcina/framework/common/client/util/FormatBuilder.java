package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormatBuilder {
	private StringBuilder sb = new StringBuilder();

	private String separator = "";

	private String prefix;

	private int indent;

	boolean indented = false;

	public void append(String string) {
		sb.append(string);
	}

	public FormatBuilder appendIfBuilderEmpty(String optional) {
		if (sb.length() == 0) {
			ensureIndent();
			sb.append(optional);
		}
		return this;
	}

	public void appendIfBuilderNonEmpty(String optional) {
		if (sb.length() > 0) {
			ensureIndent();
			sb.append(optional);
		}
	}

	public FormatBuilder appendIfNotBlank(Collection optionals) {
		for (Object optional : optionals) {
			if (optional == null) {
			} else {
				appendIfNotBlank(optional.toString());
			}
		}
		return this;
	}

	public FormatBuilder appendIfNotBlank(Object... optionals) {
		return appendIfNotBlank(Arrays.asList(optionals));
	}

	public FormatBuilder appendIfNotBlank(Stream optionals) {
		return appendIfNotBlank(optionals.collect(Collectors.toList()));
	}

	public void appendIfNotBlank(String optional) {
		if (Ax.notBlank(optional)) {
			ensureIndent();
			maybeAppendSeparator();
			sb.append(optional);
		}
	}

	public FormatBuilder format(String template, Object... args) {
		ensureIndent();
		maybeAppendSeparator();
		sb.append(Ax.format(template, args));
		return this;
	}

	public void friendly(Object toFriendly) {
		appendIfNotBlank(Ax.friendly(toFriendly));
	}

	public void indent(int indent) {
		this.indent = indent;
	}

	public FormatBuilder line(String template, Object... args) {
		return format(template, args).newLine();
	}

	public FormatBuilder newLine() {
		sb.append("\n");
		indented = false;
		return this;
	}

	public FormatBuilder prefix(String prefix) {
		this.prefix = prefix;
		appendIfBuilderEmpty(prefix);
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

	private void ensureIndent() {
		if (!indented && indent != 0) {
			indented = true;
			sb.append(CommonUtils.padStringLeft("", indent, ' '));
		}
	}

	private void maybeAppendSeparator() {
		if (sb.length() > 0 && separator.length() > 0
				&& !(prefix != null && prefix.length() == sb.length())) {
			ensureIndent();
			sb.append(separator);
		}
	}
}