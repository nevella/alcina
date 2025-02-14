/*
 * Copyright (C) 2007 The Guava Authors
 *
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
package com.google.common.base;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

/**
 * Simple static methods to be called at the start of your own methods to verify
 * correct arguments and state. This allows constructs such as
 * 
 * <pre>
 * if (count <= 0) {
 * 	throw new IllegalArgumentException("must be positive: " + count);
 * }
 * </pre>
 *
 * to be replaced with the more compact
 * 
 * <pre>
 * checkArgument(count > 0, "must be positive: %s", count);
 * </pre>
 *
 * Note that the sense of the expression is inverted; with {@code Preconditions}
 * you declare what you expect to be <i>true</i>, just as you do with an
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/language/assert.html">
 * {@code assert}</a> or a JUnit {@code assertTrue} call.
 *
 * <p>
 * <b>Warning:</b> only the {@code "%s"} specifier is recognized as a
 * placeholder in these messages, not the full range of
 * {@link String#format(String, Object[])} specifiers.
 *
 * <p>
 * Take care not to confuse precondition checking with other similar types of
 * checks! Precondition exceptions -- including those provided here, but also
 * {@link IndexOutOfBoundsException}, {@link NoSuchElementException},
 * {@link UnsupportedOperationException} and others -- are used to signal that
 * the <i>calling method</i> has made an error. This tells the caller that it
 * should not have invoked the method when it did, with the arguments it did, or
 * perhaps ever. Postcondition or other invariant failures should not throw
 * these types of exceptions.
 *
 * @author Kevin Bourrillion
 * @since 2 (imported from Google Collections Library)
 */
@GwtCompatible
public final class Preconditions {
	private static String badElementIndex(int index, int size, String desc) {
		if (index < 0) {
			return format("%s (%s) must not be negative", desc, index);
		} else if (size < 0) {
			throw new IllegalArgumentException("negative size: " + size);
		} else { // index >= size
			return format("%s (%s) must be less than size (%s)", desc, index,
					size);
		}
	}

	private static String badPositionIndex(int index, int size, String desc) {
		if (index < 0) {
			return format("%s (%s) must not be negative", desc, index);
		} else if (size < 0) {
			throw new IllegalArgumentException("negative size: " + size);
		} else { // index > size
			return format("%s (%s) must not be greater than size (%s)", desc,
					index, size);
		}
	}

	private static String badPositionIndexes(int start, int end, int size) {
		if (start < 0 || start > size) {
			return badPositionIndex(start, size, "start index");
		}
		if (end < 0 || end > size) {
			return badPositionIndex(end, size, "end index");
		}
		// end < start
		return format("end index (%s) must not be less than start index (%s)",
				end, start);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression, @Nullable
	Object errorMessage) {
		if (!expression) {
			throw new IllegalArgumentException(String.valueOf(errorMessage));
		}
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression, @Nullable
	Supplier<String> errorMessageSupplier) {
		if (!expression) {
			throw new IllegalArgumentException(errorMessageSupplier.get());
		}
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 *
	 * @since 20.0 (varargs overload since 2.0)
	 */
	public static void checkArgument(boolean expression,
			String errorMessageTemplate, int p1) {
		if (!expression) {
			throw new IllegalArgumentException(
					format(errorMessageTemplate, p1));
		}
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets
	 *            {@code errorMessageArgs[0]}, etc. Unmatched arguments will be
	 *            appended to the formatted message in square braces. Unmatched
	 *            placeholders will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 * @throws NullPointerException
	 *             if the check fails and either {@code
	 *     errorMessageTemplate} or {@code errorMessageArgs} is null (don't let
	 *             this happen)
	 */
	public static void checkArgument(boolean expression, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(
					format(errorMessageTemplate, errorMessageArgs));
		}
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>element</i> in an array,
	 * list or string of size {@code size}. An element index may range from
	 * zero, inclusive, to {@code size}, exclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying an element of an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is not less than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkElementIndex(int index, int size) {
		return checkElementIndex(index, size, "index");
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>element</i> in an array,
	 * list or string of size {@code size}. An element index may range from
	 * zero, inclusive, to {@code size}, exclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying an element of an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @param desc
	 *            the text to use to describe this index in an error message
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is not less than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkElementIndex(int index, int size, @Nullable
	String desc) {
		// Carefully optimized for execution by hotspot (explanatory comment
		// above)
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException(
					badElementIndex(index, size, desc));
		}
		return index;
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference, @Nullable
	Object errorMessage) {
		if (reference == null) {
			throw new NullPointerException(String.valueOf(errorMessage));
		}
		return reference;
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets
	 *            {@code errorMessageArgs[0]}, etc. Unmatched arguments will be
	 *            appended to the formatted message in square braces. Unmatched
	 *            placeholders will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (reference == null) {
			// If either of these parameters is null, the right thing happens
			// anyway
			throw new NullPointerException(
					format(errorMessageTemplate, errorMessageArgs));
		}
		return reference;
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>position</i> in an array,
	 * list or string of size {@code size}. A position index may range from zero
	 * to {@code size}, inclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying a position in an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is greater than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkPositionIndex(int index, int size) {
		return checkPositionIndex(index, size, "index");
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>position</i> in an array,
	 * list or string of size {@code size}. A position index may range from zero
	 * to {@code size}, inclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying a position in an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @param desc
	 *            the text to use to describe this index in an error message
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is greater than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkPositionIndex(int index, int size, @Nullable
	String desc) {
		// Carefully optimized for execution by hotspot (explanatory comment
		// above)
		if (index < 0 || index > size) {
			throw new IndexOutOfBoundsException(
					badPositionIndex(index, size, desc));
		}
		return index;
	}

	/*
	 * All recent hotspots (as of 2009) *really* like to have the natural code
	 *
	 * if (guardExpression) { throw new BadException(messageExpression); }
	 *
	 * refactored so that messageExpression is moved to a separate
	 * String-returning method.
	 *
	 * if (guardExpression) { throw new BadException(badMsg(...)); }
	 *
	 * The alternative natural refactorings into void or Exception-returning
	 * methods are much slower. This is a big deal - we're talking factors of
	 * 2-8 in microbenchmarks, not just 10-20%. (This is a hotspot optimizer
	 * bug, which should be fixed, but that's a separate, big project).
	 *
	 * The coding pattern above is heavily used in java.util, e.g. in ArrayList.
	 * There is a RangeCheckMicroBenchmark in the JDK that was used to test
	 * this.
	 *
	 * But the methods in this class want to throw different exceptions,
	 * depending on the args, so it appears that this pattern is not directly
	 * applicable. But we can use the ridiculous, devious trick of throwing an
	 * exception in the middle of the construction of another exception. Hotspot
	 * is fine with that.
	 */
	/**
	 * Ensures that {@code start} and {@code end} specify a valid
	 * <i>positions</i> in an array, list or string of size {@code size}, and
	 * are in order. A position index may range from zero to {@code size},
	 * inclusive.
	 *
	 * @param start
	 *            a user-supplied index identifying a starting position in an
	 *            array, list or string
	 * @param end
	 *            a user-supplied index identifying a ending position in an
	 *            array, list or string
	 * @param size
	 *            the size of that array, list or string
	 * @throws IndexOutOfBoundsException
	 *             if either index is negative or is greater than {@code size},
	 *             or if {@code end} is less than {@code start}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static void checkPositionIndexes(int start, int end, int size) {
		// Carefully optimized for execution by hotspot (explanatory comment
		// above)
		if (start < 0 || end < start || end > size) {
			throw new IndexOutOfBoundsException(
					badPositionIndexes(start, end, size));
		}
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression) {
		if (!expression) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression, @Nullable
	Object errorMessage) {
		if (!expression) {
			throw new IllegalStateException(String.valueOf(errorMessage));
		}
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets
	 *            {@code errorMessageArgs[0]}, etc. Unmatched arguments will be
	 *            appended to the formatted message in square braces. Unmatched
	 *            placeholders will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 * @throws NullPointerException
	 *             if the check fails and either {@code
	 *     errorMessageTemplate} or {@code errorMessageArgs} is null (don't let
	 *             this happen)
	 */
	public static void checkState(boolean expression, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalStateException(
					format(errorMessageTemplate, errorMessageArgs));
		}
	}

	/**
	 * Substitutes each {@code %s} in {@code template} with an argument. These
	 * are matched by position - the first {@code %s} gets {@code args[0]}, etc.
	 * If there are more arguments than placeholders, the unmatched arguments
	 * will be appended to the end of the formatted message in square braces.
	 *
	 * @param template
	 *            a non-null string containing 0 or more {@code %s}
	 *            placeholders.
	 * @param args
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}. Arguments can be null.
	 */
	@VisibleForTesting
	static String format(String template, @Nullable
	Object... args) {
		template = String.valueOf(template); // null -> "null"
		// start substituting the arguments into the '%s' placeholders
		StringBuilder builder = new StringBuilder(
				template.length() + 16 * args.length);
		int templateStart = 0;
		int i = 0;
		while (i < args.length) {
			int placeholderStart = template.indexOf("%s", templateStart);
			if (placeholderStart == -1) {
				break;
			}
			builder.append(template.substring(templateStart, placeholderStart));
			builder.append(args[i++]);
			templateStart = placeholderStart + 2;
		}
		builder.append(template.substring(templateStart));
		// if we run out of placeholders, append the extra args in square braces
		if (i < args.length) {
			builder.append(" [");
			builder.append(args[i++]);
			while (i < args.length) {
				builder.append(", ");
				builder.append(args[i++]);
			}
			builder.append(']');
		}
		return builder.toString();
	}

	private Preconditions() {
	}

	public static void checkArgument(boolean b, String errorMessageTemplate,
			Object p1) {
		if (!b) {
			throw new IllegalArgumentException(
					format(errorMessageTemplate, p1));
		}
	}

	public static void checkArgument(boolean b, String errorMessageTemplate,
			Object p1, Object p2) {
		if (!b) {
			throw new IllegalArgumentException(
					format(errorMessageTemplate, p1, p2));
		}
	}

	public static void checkState(boolean b, String errorMessageTemplate,
			Object p1) {
		if (!b) {
			throw new IllegalStateException(format(errorMessageTemplate, p1));
		}
	}

	public static void checkState(boolean b, String errorMessageTemplate,
			int p1) {
		if (!b) {
			throw new IllegalStateException(format(errorMessageTemplate, p1));
		}
	}

	public static void checkState(boolean b, String errorMessageTemplate,
			long p1) {
		if (!b) {
			throw new IllegalStateException(format(errorMessageTemplate, p1));
		}
	}

	public static void checkArgument(boolean b, String errorMessageTemplate,
			char p1) {
		if (!b) {
			throw new IllegalArgumentException(
					format(errorMessageTemplate, p1));
		}
	}
}