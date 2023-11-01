/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.rpc.impl;

import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * For internal use only. Used for server call serialization.
 */
public final class ClientSerializationStreamWriter2
		extends AbstractSerializationStreamWriter {
	public static String quoteString(String str) {
		return quoteStringHosted(str);
	}

	public static String quoteStringHosted(String str) {
		String quotingRegexString = "/[\\u0000\\|\\\\\\\uD800-\uFFFF]/g";
		RegExp regExp = RegExp.compile(quotingRegexString);
		int idx = 0;
		StringBuilder out = new StringBuilder();
		MatchResult result;
		while ((result = regExp.exec(str)) != null) {
			out.append(str.substring(idx, result.getIndex()));
			idx = result.getIndex() + 1;
			char ch = result.getGroup(0).charAt(0);
			if (ch == 0) {
				out.append("\\0");
			} else if (ch == 92) { // backslash
				out.append("\\\\");
			} else if (ch == 124) { // vertical bar
				// 124 = "|" = AbstractSerializationStream.RPC_SEPARATOR_CHAR
				out.append("\\!");
			} else {
				String hex = Integer.toHexString(ch);
				out.append("\\u0000".substring(0, 6 - hex.length()) + hex);
			}
		}
		return out + str.substring(idx);
	}

	private static void append(StringBuilder sb, String token) {
		assert (token != null);
		sb.append(token);
		sb.append(RPC_SEPARATOR_CHAR);
	}

	private StringBuilder encodeBuffer;

	private final String moduleBaseURL;

	private final String serializationPolicyStrongName;

	private final Serializer serializer;

	/**
	 * Constructs a <code>ClientSerializationStreamWriter</code> using the
	 * specified module base URL and the serialization policy.
	 * 
	 * @param serializer
	 *            the {@link Serializer} to use
	 * @param moduleBaseURL
	 *            the location of the module
	 * @param serializationPolicyStrongName
	 *            the strong name of serialization policy
	 */
	public ClientSerializationStreamWriter2(Serializer serializer,
			String moduleBaseURL, String serializationPolicyStrongName) {
		this.serializer = serializer;
		this.moduleBaseURL = moduleBaseURL;
		this.serializationPolicyStrongName = serializationPolicyStrongName;
	}

	/**
	 * Call this method before attempting to append any tokens. This method
	 * implementation <b>must</b> be called by any overridden version.
	 */
	@Override
	public void prepareToWrite() {
		super.prepareToWrite();
		encodeBuffer = new StringBuilder();
		// Write serialization policy info
		writeString(moduleBaseURL);
		writeString(serializationPolicyStrongName);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		writeHeader(buffer);
		writeStringTable(buffer);
		writePayload(buffer);
		return buffer.toString();
	}

	@Override
	public void writeLong(long value) {
		append(longToBase64(value));
	}

	private void writeHeader(StringBuilder buffer) {
		append(buffer, String.valueOf(getVersion()));
		append(buffer, String.valueOf(getFlags()));
	}

	private void writePayload(StringBuilder buffer) {
		buffer.append(encodeBuffer.toString());
	}

	private StringBuilder writeStringTable(StringBuilder buffer) {
		List<String> stringTable = getStringTable();
		append(buffer, String.valueOf(stringTable.size()));
		for (String s : stringTable) {
			append(buffer, quoteString(s));
		}
		return buffer;
	}

	/**
	 * Appends a token to the end of the buffer.
	 */
	@Override
	protected void append(String token) {
		append(encodeBuffer, token);
	}

	@Override
	protected String getObjectTypeSignature(Object o) {
		Class<?> clazz = o.getClass();
		if (o instanceof Enum<?>) {
			Enum<?> e = (Enum<?>) o;
			clazz = e.getDeclaringClass();
		}
		return serializer.getSerializationSignature(clazz);
	}

	@Override
	protected void serialize(Object instance, String typeSignature)
			throws SerializationException {
		serializer.serialize(this, instance, typeSignature);
	}
}
