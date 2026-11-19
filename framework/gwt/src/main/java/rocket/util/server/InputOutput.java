/*
 * Copyright Miroslav Pokorny
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
package rocket.util.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import rocket.util.client.Checker;

/**
 * A collection of useful helper methods relating to IO such as closing
 * streams/readers/writers etc.
 * 
 * @author Miroslav Pokorny
 * @version 1.0
 */
public class InputOutput {
	/**
	 * Closes a previously open InputStream if necessary
	 * 
	 * @param stream
	 *            The InputStream which may be null
	 */
	public static void closeIfNecessary(final InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (final IOException closing) {
				closing.printStackTrace();
			}
		}
	}

	/**
	 * Closes and flushes a previously open OutputStream if necessary
	 * 
	 * @param stream
	 *            The OutputStream which may be null
	 */
	public static void closeIfNecessary(final OutputStream stream) {
		if (stream != null) {
			try {
				stream.flush();
			} catch (final IOException closing) {
				closing.printStackTrace();
			}
			try {
				stream.close();
			} catch (final IOException closing) {
				closing.printStackTrace();
			}
		}
	}

	/**
	 * Closes a previously open PrintWriter if necessary
	 * 
	 * @param printWriter
	 *            The PrintWriter which may be null
	 */
	public static void closeIfNecessary(final PrintWriter printWriter) {
		if (printWriter != null) {
			try {
				printWriter.flush();
			} catch (final Exception flushProblem) {
				flushProblem.printStackTrace();
			}
			try {
				printWriter.close();
			} catch (final Exception closing) {
				closing.printStackTrace();
			}
		}
	}

	/**
	 * Closes a previously open Reader if necessary
	 * 
	 * @param reader
	 *            The Reader which may be null
	 */
	public static void closeIfNecessary(final Reader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (final IOException closing) {
				closing.printStackTrace();
			}
		}
	}

	/**
	 * Closes a previously open Writer if necessary
	 * 
	 * @param writer
	 *            The Writer which may be null
	 */
	public static void closeIfNecessary(final Writer writer) {
		if (writer != null) {
			try {
				writer.flush();
			} catch (final IOException flushProblem) {
				flushProblem.printStackTrace();
			}
			try {
				writer.close();
			} catch (final IOException closing) {
				closing.printStackTrace();
			}
		}
	}

	/**
	 * Reconstitutes an object given its serialized byte form.
	 * 
	 * @param bytes
	 *            An array of bytes containing the object.
	 * @return The deserialized object.
	 * @throws UncheckedIOException
	 *             if anything goes wrong.
	 */
	public static Object deserialize(final byte[] bytes)
			throws UncheckedIOException {
		ByteArrayInputStream bytesInputStream = null;
		try {
			bytesInputStream = new ByteArrayInputStream(bytes);
			final ObjectInputStream objectOutput = new ObjectInputStream(
					bytesInputStream);
			return objectOutput.readObject();
		} catch (final ClassNotFoundException caught) {
			throwIOException(
					"A problem occured when attempting to deserialize " + bytes,
					caught);
			return null;
		} catch (final IOException caught) {
			throwIOException(
					"A problem occured when attempting to deserialize " + bytes,
					caught);
			return null;
		} finally {
			InputOutput.closeIfNecessary(bytesInputStream);
		}
	}

	/**
	 * Wraps the given reader with a BufferedReader if necessary
	 * 
	 * @param reader
	 *            Reader
	 * @return A guaranteed BufferedReader
	 */
	public static BufferedReader makeReaderBuffered(final Reader reader) {
		Checker.notNull("parameter:reader", reader);
		return reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader);
	}

	/**
	 * Wraps the given writer with a BufferedWriter if necessary
	 * 
	 * @param writer
	 *            Writer
	 * @return A guaranteed BufferedWriter
	 */
	public static BufferedWriter makeWriterBuffered(final Writer writer) {
		Checker.notNull("parameter:writer", writer);
		return writer instanceof BufferedWriter ? (BufferedWriter) writer
				: new BufferedWriter(writer);
	}

	/**
	 * Asserts that the given object is not null and is serializable.
	 * 
	 * @param name
	 *            String
	 * @param object
	 *            Object
	 */
	public static void mustBeSerializable(final String name,
			final Object object) {
		Checker.notNull(name, object);
		if (false == (object instanceof java.io.Serializable)) {
			Checker.fail(name,
					"The " + name + " is not serializable, object: " + object);
		}
	}

	/**
	 * Takes an object and returns its serialized form as a series of bytes.
	 * This method supports serializing of null object references.
	 * 
	 * This method takes care of the messy details such preparing a
	 * ByteArrayOutputStream and ObjectOutputStream, cleanup etc.
	 * 
	 * @param object
	 *            Serializable
	 * @return byte[]
	 * @throws IOException
	 */
	public static byte[] nullSafeSerialize(final Serializable object)
			throws UncheckedIOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream objectOutput = new ObjectOutputStream(
					bytes);
			objectOutput.writeObject(object);
			return bytes.toByteArray();
		} catch (final IOException caught) {
			throwIOException(
					"A problem occured when attempting to serialize " + object,
					caught);
			return null;
		} finally {
			InputOutput.closeIfNecessary(bytes);
		}
	}

	/**
	 * Takes an object and returns its serialized form as a series of bytes. If
	 * one wishes to possibly serialize null objects use
	 * {@link #nullSafeSerialize}due to the inclusion of an assertion test when
	 * entering the method.
	 * 
	 * This method takes care of the messy details such preparing a
	 * ByteArrayOutputStream and ObjectOutputStream, cleanup etc.
	 * 
	 * @param object
	 *            This object should or must implement Serializable.
	 * @return The resulting bytes.
	 * @throws UncheckedIOException
	 *             if nything goes wrong when serializing (should pretty much
	 *             always work)
	 */
	public static byte[] serialize(final Serializable object)
			throws UncheckedIOException {
		Checker.notNull("parameter:object", object);
		return nullSafeSerialize(object);
	}

	/**
	 * May be used to report any IO related Exceptions converting them into an
	 * equivalent unchecked exception.
	 * 
	 * @param message
	 *            String
	 * @param cause
	 *            Throwable
	 */
	public static void throwIOException(final String message,
			final Throwable cause) {
		Checker.notEmpty("assert:message", message);
		Checker.notNull("assert:cause", cause);
		throw new UncheckedIOException(message, cause);
	}

	/**
	 * May be used to report any IO related Exceptions converting them into an
	 * equivalent unchecked exception.
	 * 
	 * @param cause
	 *            Throwable
	 */
	public static void throwIOException(final Throwable cause) {
		Checker.notNull("assert:cause", cause);
		throw new UncheckedIOException(cause);
	}

	/**
	 * No need to create
	 */
	private InputOutput() {
	}
}