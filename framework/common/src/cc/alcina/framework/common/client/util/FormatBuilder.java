package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

/**
 * <li>Helper class based on StringBuilder to create strings with variable
 * substituions (similar to `String.format()`)</li>
 * <li>Can also prepend a prefix to the entire builder block, append separators
 * and add consistent indentation</li>
 */
public class FormatBuilder {
	public static String keyValues(Object... args) {
		return new FormatBuilder().separator("  ").appendKeyValues(args)
				.toString();
	}

	/**
	 * Internal string bugger
	 */
	private StringBuilder sb = new StringBuilder();

	/**
	 * Separator to append after each `format()` call
	 */
	private String separator = "";

	private String prefix;

	/**
	 * Indent level to prepend on to every line
	 */
	private int indent;

	/**
	 * Whether the current line has been indented or not
	 */
	boolean indented = false;

	/**
	 * Append an object as a string to the end of the buffer
	 *
	 * @param object
	 *            Object to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder append(Object object) {
		format(CommonUtils.nullSafeToString(object));
		return this;
	}

	/**
	 * Append a multi-line block of text to the buffer
	 *
	 * @param text
	 *            Mutli-line block of text to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendBlock(String text) {
		// GWT jjs doesn't like this::line (because varargs)
		Arrays.stream(text.split("\n")).forEach(l -> this.line(l));
		return this;
	}

	/**
	 * Append string to buffer if `test` is true
	 *
	 * @param test
	 *            Whether to append string
	 * @param string
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendIf(boolean test, String string) {
		if (test) {
			format(string);
		}
		return this;
	}

	/**
	 * Append string to buffer only if the buffer is empty
	 *
	 * @param optional
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendIfBuilderEmpty(String optional) {
		if (sb.length() == 0) {
			ensureIndent();
			sb.append(optional);
		}
		return this;
	}

	/**
	 * Append string to buffer only if the buffer is NOT empty
	 *
	 * @param optional
	 *            String to append
	 * @return This FormatBuilder
	 */
	public void appendIfBuilderNonEmpty(String optional) {
		if (sb.length() > 0) {
			ensureIndent();
			sb.append(optional);
		}
	}

	public <T> void appendIfNonNull(T t, Function<T, ?> nonNullMapper) {
		if (t != null) {
			append(nonNullMapper.apply(t));
		}
	}

	/**
	 * Append all strings in a Collection only if they are not blank
	 *
	 * @param optionals
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendIfNotBlank(Collection optionals) {
		for (Object optional : optionals) {
			if (optional != null) {
				appendIfNotBlank(optional.toString());
			}
		}
		return this;
	}

	/**
	 * Append all strings in arguments only if they are not blank
	 *
	 * @param optionals
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendIfNotBlank(Object... optionals) {
		return appendIfNotBlank(Arrays.asList(optionals));
	}

	/**
	 * Append all strings in a Stream only if they are not blank
	 *
	 * @param optionals
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendIfNotBlank(Stream optionals) {
		return appendIfNotBlank(optionals.collect(Collectors.toList()));
	}

	/**
	 * Append only if the string is not blank
	 *
	 * @param optionals
	 *            String to append
	 * @return This FormatBuilder
	 */
	public void appendIfNotBlank(String optional) {
		if (Ax.notBlank(optional)) {
			ensureIndent();
			maybeAppendSeparator();
			sb.append(optional);
		}
	}

	/**
	 * If value is not blank, append a simple key-value representation
	 */
	public FormatBuilder appendIfNotBlankKv(String key, Object value) {
		String toString = value == null ? null : value.toString();
		if (Ax.notBlank(toString)) {
			append(key);
			sb.append(":");
			sb.append(toString);
		}
		return this;
	}

	public void appendIfNotBlankKv(String key, Object object,
			Supplier supplier) {
		if (object != null) {
			appendIfNotBlankKv(key, supplier.get());
		}
	}

	public FormatBuilder appendKeyValues(Object... objects) {
		Preconditions.checkState(objects.length % 2 == 0);
		for (int idx = 0; idx < objects.length; idx += 2) {
			String key = (String) objects[idx];
			Object value = objects[idx + 1];
			appendIfNotBlankKv(key, value);
		}
		return this;
	}

	/**
	 * Append object as string with a left pad of `width` spaces
	 *
	 * @param width
	 *            Number of spaces to append
	 * @param object
	 *            Object to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendPadLeft(int width, Object object) {
		sb.append(CommonUtils.padStringLeft(
				CommonUtils.nullSafeToString(object), width, " "));
		return this;
	}

	/**
	 * Append object as string with a left pad of `width` spaces
	 *
	 * @param width
	 *            Number of spaces to append
	 * @param object
	 *            Object to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendPadRight(int width, Object object) {
		sb.append(CommonUtils.padStringRight(
				CommonUtils.nullSafeToString(object), width, ' '));
		return this;
	}

	/**
	 * <li>Append string to buffer without a separator</li>
	 * <li>Also ignores indent</li>
	 *
	 * @param string
	 *            String to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendWithoutSeparator(String string) {
		sb.append(string);
		return this;
	}

	/**
	 * Append object as string with a left pad of `width` zeroes
	 *
	 * @param width
	 *            Number of zeroes to append
	 * @param value
	 *            the int to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder appendZeroesLeft(int width, int value) {
		sb.append(CommonUtils.padStringLeft(String.valueOf(value), width, "0"));
		return this;
	}

	/**
	 * call format(template,args) if test is true
	 *
	 * @return This FormatBuilder
	 */
	public FormatBuilder conditionalFormat(boolean test, String template,
			Object... args) {
		if (test) {
			return format(template, args);
		} else {
			return this;
		}
	}

	/**
	 * Append a collection, one line per element
	 */
	public void elementLines(Collection<?> collection) {
		collection.forEach(this::line);
	}

	/**
	 * If an indent is present, ensure the current line is indented
	 */
	private void ensureIndent() {
		if (!indented && indent != 0) {
			indented = true;
			sb.append(CommonUtils.padStringLeft("", indent, ' '));
		}
	}

	/**
	 * Fill the current line with a fill string, `width` times
	 *
	 * @param width
	 *            Number of times to append the fill string
	 * @param fill
	 *            String to append multiple times
	 * @return This FormatBuilder
	 */
	public FormatBuilder fill(int width, String fill) {
		sb.append(CommonUtils.padStringLeft("", width, fill));
		sb.append("\n");
		return this;
	}

	/**
	 * <li>Append a formatted template string with given args</li>
	 * <li>Use similar to `String.format()`
	 *
	 * @param template
	 *            Template string to append
	 * @param args
	 *            Arguments to apply to template string
	 * @return This FormatBuilder
	 */
	public FormatBuilder format(String template, Object... args) {
		ensureIndent();
		maybeAppendSeparator();
		sb.append(args.length == 0 ? template : Ax.format(template, args));
		return this;
	}

	/**
	 * <li>Append object as 'friendly' form to buffer</li>
	 * <li>See `Ax.friendly()` for more info</li>
	 *
	 * @param toFriendly
	 *            Object to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder friendly(Object toFriendly) {
		appendIfNotBlank(Ax.friendly(toFriendly));
		return this;
	}

	/**
	 * <li>Set indent level to apply to each line</li>
	 * <li>Only applies to lines after this call</li>
	 *
	 * @param indent
	 *            Number of spaces to indent new lines
	 * @return This FormatBuilder
	 */
	public FormatBuilder indent(int indent) {
		this.indent = indent;
		return this;
	}

	/**
	 * Return the length of the current string
	 *
	 * @return Length of the current string
	 */
	public int length() {
		return this.sb.length();
	}

	/**
	 * Append object as string and insert a new line after
	 *
	 * @param object
	 *            Object to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder line(Object object) {
		return append(object).newLine();
	}

	/**
	 * <li>Append a formatted template string with given args, and insert a new
	 * line after</li>
	 * <li>Use similar to `String.format()`
	 *
	 * @param template
	 *            Template string to append
	 * @param args
	 *            Arguments to apply to template string
	 * @return This FormatBuilder
	 */
	public FormatBuilder line(String template, Object... args) {
		return format(template, args).newLine();
	}

	/**
	 * Add a separator if the buffer is not empty and a separator is set
	 */
	private void maybeAppendSeparator() {
		if (sb.length() > 0 && separator.length() > 0
				&& !(prefix != null && prefix.length() == sb.length())) {
			ensureIndent();
			sb.append(separator);
		}
	}

	/**
	 * Append a new line
	 *
	 * @return This FormatBuilder
	 */
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

	/**
	 * <li>Set separator to apply after before each string except the first</li>
	 * <li>Only applies to lines after this call</li>
	 *
	 * @param separator
	 *            Separator to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder separator(String separator) {
		this.separator = separator;
		return this;
	}

	/**
	 * Append a string followed by a new line with dashes
	 *
	 * @param title
	 *            Title string to append
	 * @return This FormatBuilder
	 */
	public FormatBuilder title(String title) {
		sb.append(title);
		sb.append('\n');
		IntStream.range(0, title.length()).forEach(i -> sb.append('-'));
		sb.append('\n');
		return this;
	}

	/**
	 * Return the string buffer
	 *
	 * @return Built string
	 */
	@Override
	public String toString() {
		return sb.toString();
	}
}