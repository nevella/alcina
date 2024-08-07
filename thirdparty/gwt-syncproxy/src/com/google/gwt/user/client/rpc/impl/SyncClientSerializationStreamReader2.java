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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsValueLiteral;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.Base64Utils;

/**
 * For internal use only. Used for server call serialization.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
public final class SyncClientSerializationStreamReader2
		extends AbstractSerializationStreamReader2 {
	private RpcDecoder decoder;

	int index;

	Serializer serializer;

	AsyncCallback postPrepareCallback;

	public SyncClientSerializationStreamReader2(Serializer serializer) {
		this.serializer = serializer;
	}

	public void doDeserialize(AsyncCallback postPrepareCallback) {
		this.postPrepareCallback = postPrepareCallback;
		AsyncDeserializerDev asyncDeserializerDev = new AsyncDeserializerDev(
				this);
		while (asyncDeserializerDev.execute())
			;
	}

	public List<Object> getSeenArray() {
		return seenArray;
	}

	public int getTypeId(int index) {
		return decoder.typeTable.get(index);
	}

	public int getTypeTableLength() {
		return decoder.typeTable.size();
	}

	@Override
	public void prepareToRead(String encoded) throws SerializationException {
		try {
			List<JsStatement> stmts = JsParser.parse(SourceOrigin.UNKNOWN,
					JsRootScope.INSTANCE, new StringReader(encoded));
			ArrayConcatEvaler arrayConcatEvaler = new ArrayConcatEvaler();
			arrayConcatEvaler.acceptList(stmts);
			StringConcatEvaler stringConcatEvaler = new StringConcatEvaler();
			stringConcatEvaler.acceptList(stmts);
			decoder = new RpcDecoder();
			decoder.acceptList(stmts);
		} catch (Exception e) {
			throw new SerializationException("Failed to parse RPC payload", e);
		}
		index = decoder.getValues().size();
		super.prepareToRead(encoded);
		if (getVersion() < SERIALIZATION_STREAM_MIN_VERSION
				|| getVersion() > SERIALIZATION_STREAM_MAX_VERSION) {
			throw new IncompatibleRemoteServiceException("Got version "
					+ getVersion() + ", expected version between "
					+ SERIALIZATION_STREAM_MIN_VERSION + " and "
					+ SERIALIZATION_STREAM_MAX_VERSION);
		}
		if (((getFlags() & FLAG_INCREMENTAL_DESERIALIZABLE) == 0
				|| getVersion() != SERIALIZATION_STREAM_VERSION)) {
			throw new IncompatibleRemoteServiceException(
					"requires incremental deserializable rpc");
		}
		if (!areFlagsValid()) {
			throw new IncompatibleRemoteServiceException(
					"Got an unknown flag from " + "server: " + getFlags());
		}
	}

	@Override
	public boolean readBoolean() {
		JsValueLiteral literal = decoder.getValues().get(--index);
		return literal.isBooleanTrue();
	}

	@Override
	public byte readByte() {
		JsNumberLiteral literal = (JsNumberLiteral) decoder.getValues()
				.get(--index);
		return (byte) literal.getValue();
	}

	@Override
	public char readChar() {
		JsNumberLiteral literal = (JsNumberLiteral) decoder.getValues()
				.get(--index);
		return (char) literal.getValue();
	}

	@Override
	public double readDouble() {
		JsValueLiteral valueLiteral = decoder.getValues().get(--index);
		if (valueLiteral instanceof JsNumberLiteral) {
			JsNumberLiteral literal = (JsNumberLiteral) valueLiteral;
			return literal.getValue();
		} else if (valueLiteral instanceof JsStringLiteral) {
			JsStringLiteral literal = (JsStringLiteral) valueLiteral;
			return Double.parseDouble(literal.getValue());
		} else {
			throw new RuntimeException("Can't read double from "
					+ valueLiteral.getKind() + " literal");
		}
	}

	@Override
	public float readFloat() {
		return (float) readDouble();
	}

	@Override
	public int readInt() {
		JsNumberLiteral literal = (JsNumberLiteral) decoder.getValues()
				.get(--index);
		return (int) literal.getValue();
	}

	@Override
	public long readLong() {
		return Base64Utils.longFromBase64(
				((JsStringLiteral) decoder.getValues().get(--index))
						.getValue());
	}

	@Override
	public Object readObject() throws SerializationException {
		// at the end of the deserialization, return root
		if (index == 0) {
			return seenArray.get(0);
		}
		return super.readObject();
	}

	@Override
	public short readShort() {
		JsNumberLiteral literal = (JsNumberLiteral) decoder.getValues()
				.get(--index);
		return (short) literal.getValue();
	}

	@Override
	public String readString() {
		return getString(readInt());
	}

	@Override
	protected Object deserialize(String typeSignature)
			throws SerializationException {
		int id = reserveDecodedObjectIndex();
		Object instance = serializer.instantiate(this, typeSignature);
		rememberDecodedObject(id, instance);
		serializer.deserialize(this, instance, typeSignature);
		return instance;
	}

	@Override
	protected String getString(int index) {
		// index is 1-based
		return index > 0 ? decoder.getStringTable().get(index - 1) : null;
	}

	/**
	 * The server breaks up large arrays in the RPC payload into smaller arrays
	 * using concat() expressions. For example, [1, 2, 3, 4, 5, 6, 7] can be
	 * broken up into [1, 2].concat([3, 4], [5, 6], [7,8])
	 * <p>
	 * This visitor reverses that transform by reducing all concat invocations
	 * into a single array literal.
	 */
	private static class ArrayConcatEvaler extends JsModVisitor {
		private List<Object> added = new ArrayList<Object>();

		@Override
		public boolean visit(JsInvocation invoke, JsContext ctx) {
			JsExpression expr = invoke.getQualifier();
			if (!(expr instanceof JsNameRef)) {
				return super.visit(invoke, ctx);
			}
			JsNameRef name = (JsNameRef) expr;
			if (!name.getIdent().equals("concat")) {
				return super.visit(invoke, ctx);
			}
			List<JsExpression> args = invoke.getArguments();
			if (added.contains(args)) {
				return super.visit(invoke, ctx);
			}
			added.add(args);
			JsArrayLiteral headElements = (JsArrayLiteral) name.getQualifier();
			for (JsExpression ex : args) {
				JsArrayLiteral arg = (JsArrayLiteral) ex;
				headElements.getExpressions().addAll(arg.getExpressions());
			}
			ctx.replaceMe(headElements);
			return true;
		}
	}

	/**
	 * Decodes an RPC payload from a JS string. There is currently no design
	 * document that describes the payload, instead you must infer it from
	 * reading ServerSerializationStreamWriter. I'll briefly describe the
	 * payload here:
	 * <p>
	 * The server sends down a string of JavaScript which is eval'ed by the
	 * webmode version of ClientSerializationStreamReader into an array. The
	 * array contains primitive values, followed by a nested array of strings,
	 * followed by a couple of header primitive values. For example,
	 *
	 * <pre>
	 * [ 1, 0, 3, -7, 13, [ "string one", "string two", "string three" ], 7, 0 ]
	 * </pre>
	 *
	 * Long primitives are encoded as strings in the outer array, and strings in
	 * the string table are referenced by index values in the outer array.
	 * <p>
	 * The payload is almost a JSON literal except for some nuances, like
	 * unicode and array concats. We have a specialized devmode version to
	 * decode this payload, because the webmode version requires multiple
	 * round-trips between devmode and the JSVM for every single element in the
	 * payload. This can require several seconds to decode a single RPC payload.
	 * The RpcDecoder operates by doing a limited JS parse on the payload within
	 * the devmode VM using Rhino, avoiding the cross-process RPCs.
	 * <p>
	 */
	private static class RpcDecoder extends JsVisitor {
		private static final String JS_INFINITY_LITERAL = "Infinity";

		private static final String JS_NAN_LITERAL = "NaN";

		State state = State.EXPECTING_PAYLOAD_BEGIN;

		List<String> stringTable = new ArrayList<String>();

		List<Integer> typeTable = new ArrayList<Integer>();

		List<JsValueLiteral> values = new ArrayList<JsValueLiteral>();

		boolean negative;

		@Override
		public void endVisit(JsArrayLiteral x, JsContext ctx) {
			if (state == State.IN_TYPE_TABLE) {
				state = State.EXPECTING_STRING_TABLE;
			}
			if (state == State.IN_STRING_TABLE) {
				state = State.EXPECTING_END;
			}
		}

		public List<String> getStringTable() {
			return stringTable;
		}

		public List<JsValueLiteral> getValues() {
			return values;
		}

		@Override
		public boolean visit(JsArrayLiteral x, JsContext ctx) {
			switch (state) {
			case EXPECTING_PAYLOAD_BEGIN:
				state = State.EXPECTING_TYPE_TABLE;
				return true;
			case EXPECTING_TYPE_TABLE:
				state = State.IN_TYPE_TABLE;
				return true;
			case EXPECTING_STRING_TABLE:
				state = State.IN_STRING_TABLE;
				return true;
			default:
				throw new RuntimeException(
						"Unexpected array in RPC payload. The string table has "
								+ "already started.");
			}
		}

		@Override
		public boolean visit(JsBooleanLiteral x, JsContext ctx) {
			values.add(x);
			return true;
		}

		@Override
		public boolean visit(JsNameRef x, JsContext ctx) {
			String ident = x.getIdent();
			if (ident.equals(JS_NAN_LITERAL)) {
				values.add(
						new JsNumberLiteral(SourceOrigin.UNKNOWN, Double.NaN));
			} else if (ident.equals(JS_INFINITY_LITERAL)) {
				double val = negative ? Double.NEGATIVE_INFINITY
						: Double.POSITIVE_INFINITY;
				negative = false;
				values.add(new JsNumberLiteral(SourceOrigin.UNKNOWN, val));
			} else {
				throw new RuntimeException("Unexpected identifier: " + ident);
			}
			return true;
		}

		@Override
		public boolean visit(JsNumberLiteral x, JsContext ctx) {
			if (state == State.IN_TYPE_TABLE) {
				typeTable.add((int) x.getValue());
				return true;
			}
			if (negative) {
				x = new JsNumberLiteral(x.getSourceInfo(), -x.getValue());
				negative = false;
			}
			values.add(x);
			return true;
		}

		@Override
		public boolean visit(JsPostfixOperation x, JsContext ctx) {
			throw new RuntimeException(
					"Unexpected postfix operator: " + x.toSource());
		}

		@Override
		public boolean visit(JsPrefixOperation x, JsContext ctx) {
			if (x.getOperator().equals(JsUnaryOperator.NEG)) {
				negative = !negative;
				return true;
			}
			// Lots of prefix operators, but we only see negatives for literals
			throw new RuntimeException(
					"Unexpected prefix operator: " + x.toSource());
		}

		@Override
		public boolean visit(JsStringLiteral x, JsContext ctx) {
			if (state == State.IN_STRING_TABLE) {
				stringTable.add(x.getValue());
			} else {
				values.add(x);
			}
			return true;
		}

		enum State {
			EXPECTING_PAYLOAD_BEGIN, EXPECTING_TYPE_TABLE, IN_TYPE_TABLE,
			EXPECTING_STRING_TABLE, IN_STRING_TABLE, EXPECTING_END
		}
	}

	/**
	 * The server splits up string literals into 64KB chunks using '+'
	 * operators. For example ['chunk1chunk2'] is broken up into ['chunk1' +
	 * 'chunk2'].
	 * <p>
	 * This visitor reverses that transform by reducing such strings into a
	 * single string literal.
	 */
	private static class StringConcatEvaler extends JsModVisitor {
		@Override
		public boolean visit(JsBinaryOperation x, JsContext ctx) {
			if (x.getOperator() != JsBinaryOperator.ADD) {
				return super.visit(x, ctx);
			}
			// Do a first pass to get the total string length to avoid
			// dynamically resizing the buffer.
			int stringLength = getStringLength(x);
			if (stringLength >= 0) {
				StringBuilder builder = new StringBuilder(stringLength);
				if (expressionToString(x, builder)) {
					ctx.replaceMe(new JsStringLiteral(x.getSourceInfo(),
							builder.toString()));
				}
			}
			return true;
		}

		/**
		 * Transforms an expression into a string. This will recurse into
		 * JsBinaryOperations of type JsBinaryOperator.ADD, which may have other
		 * ADD operations or JsStringLiterals as arguments.
		 *
		 * @param expression
		 *            the expression to evaluate
		 * @param builder
		 *            a builder that the string will be appended to
		 * @return true if the expression represents a valid string, or false
		 *         otherwise
		 */
		private boolean expressionToString(JsExpression expression,
				StringBuilder builder) {
			if (expression instanceof JsStringLiteral) {
				builder.append(((JsStringLiteral) expression).getValue());
				return true;
			}
			if (expression instanceof JsBinaryOperation) {
				JsBinaryOperation operation = (JsBinaryOperation) expression;
				if (operation.getOperator() != JsBinaryOperator.ADD) {
					return false;
				}
				return expressionToString(operation.getArg1(), builder)
						&& expressionToString(operation.getArg2(), builder);
			}
			return false;
		}

		/**
		 * Gets the total string length of the given expression. This will
		 * recurse into JsBinaryOperations of type JsBinaryOperator.ADD, which
		 * may have other ADD operations or JsStringLiterals as arguments.
		 *
		 * @param expression
		 *            the expression to evaluate
		 * @return the total string length, or -1 if the given expression does
		 *         not represent a valid string
		 */
		private int getStringLength(JsExpression expression) {
			if (expression instanceof JsStringLiteral) {
				return ((JsStringLiteral) expression).getValue().length();
			}
			if (expression instanceof JsBinaryOperation) {
				JsBinaryOperation operation = (JsBinaryOperation) expression;
				if (operation.getOperator() != JsBinaryOperator.ADD) {
					return -1;
				}
				int arg1Length = getStringLength(operation.getArg1());
				int arg2Length = getStringLength(operation.getArg2());
				return (arg1Length >= 0 && arg2Length >= 0)
						? (arg1Length + arg2Length)
						: -1;
			}
			return -1;
		}
	}

	class AsyncDeserializerDev {
		private int typeTableLength;

		private final SyncClientSerializationStreamReader2 reader;

		private int sliceSize = 500;

		private int idx2;

		private PhaseDev phase = PhaseDev.INSTATIATE_EMPTY_SETUP;

		private int size;

		public AsyncDeserializerDev(
				SyncClientSerializationStreamReader2 reader) {
			this.reader = reader;
			try {
				mkmClazz = Class.forName(
						"cc.alcina.framework.common.client.util.MultikeyMap");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public boolean execute() {
			try {
				switch (phase) {
				case INSTATIATE_EMPTY_SETUP:
					typeTableLength = reader.getTypeTableLength();
					idx2 = 0;
					phase = PhaseDev.INSTATIATE_EMPTY_RUN;
					// deliberate fallthrough
				case INSTATIATE_EMPTY_RUN:
					if (instantiateEmptyObjects()) {
						phase = PhaseDev.DESERIALIZE_NON_COLLECTION_PRE;
					} else {
						break;
					}
				case DESERIALIZE_NON_COLLECTION_PRE:
					size = reader.getSeenArray().size();
					idx2 = 0;
					phase = PhaseDev.DESERIALIZE_NON_COLLECTION_RUN;
					// deliberate fallthrough
				case DESERIALIZE_NON_COLLECTION_RUN:
					if (deserializeProperties()) {
						phase = PhaseDev.DESERIALIZE_COLLECTION_PRE;
					} else {
						break;
					}
				case DESERIALIZE_COLLECTION_PRE:
					idx2 = 0;
					phase = PhaseDev.DESERIALIZE_COLLECTION_RUN;
					// deliberate fallthrough
				case DESERIALIZE_COLLECTION_RUN:
					if (deserializeProperties()) {
						reader.postPrepareCallback.onSuccess(null);
						return false;
					} else {
						break;
					}
				}
			} catch (final Throwable e) {
				reader.postPrepareCallback.onFailure(e);
				return false;
			}
			return true;
		}

		Class mkmClazz;

		private boolean deserializeProperties() throws SerializationException {
			int sliceCount = sliceSize;
			for (; sliceCount != 0 && idx2 < size; idx2++) {
				Object instance = reader.getSeenArray().get(idx2);
				// keep in sync with asyncdeserializer, clientserreader,
				// serverserwriter
				boolean collectionOrMap = instance != null
						&& (instance instanceof Collection
								|| instance instanceof Map || mkmClazz
										.isAssignableFrom(instance.getClass()));
				if (collectionOrMap
						^ (phase == PhaseDev.DESERIALIZE_COLLECTION_RUN)) {
					continue;
				}
				if (idx2 == 0) {
					int toss = reader.readInt();// bypasss first object
				}
				int strId = reader.getTypeId(idx2);
				String typeSignature = reader.getString(strId);
				reader.serializer.deserialize(reader, instance, typeSignature);
				sliceCount--;
			}
			return idx2 == size;
		}

		private boolean instantiateEmptyObjects()
				throws SerializationException {
			int sliceCount = sliceSize;
			for (; sliceCount != 0 && idx2 < typeTableLength; idx2++) {
				int strId = reader.getTypeId(idx2);
				String typeSignature = reader.getString(strId);
				int id = reader.reserveDecodedObjectIndex();
				Object instance = reader.serializer.instantiate(reader,
						typeSignature);
				reader.rememberDecodedObject(id, instance);
				sliceCount--;
			}
			return idx2 == typeTableLength;
		}
	}

	enum PhaseDev {
		INSTATIATE_EMPTY_SETUP, INSTATIATE_EMPTY_RUN,
		DESERIALIZE_NON_COLLECTION_PRE, DESERIALIZE_NON_COLLECTION_RUN,
		DESERIALIZE_COLLECTION_PRE, DESERIALIZE_COLLECTION_RUN
	}
}
