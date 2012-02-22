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

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * For internal use only. Used for server call serialization.
 */
public final class ClientSerializationStreamReader extends
		AbstractSerializationStreamReader {
	private static native JavaScriptObject eval(String encoded) /*-{
		return eval(encoded);
	}-*/;

	private static native int getLength(JavaScriptObject array) /*-{
		return array.length;
	}-*/;

	int index;

	JavaScriptObject results;

	JavaScriptObject stringTable;

	JsArrayInteger typeTable;

	private Serializer serializer;

	public ClientSerializationStreamReader(Serializer serializer) {
		this.serializer = serializer;
	}

	private static final int SERIALIZATION_STREAM_VERSION = 1007;

	@Override
	public void prepareToRead(String encoded) throws SerializationException {
		results = eval(encoded);
		index = getLength(results);
		super.prepareToRead(encoded);
		if (getVersion() != SERIALIZATION_STREAM_VERSION) {
			throw new IncompatibleRemoteServiceException("Expecting version "
					+ SERIALIZATION_STREAM_VERSION + " from server, got "
					+ getVersion() + ".");
		}
		if (!areFlagsValid()) {
			throw new IncompatibleRemoteServiceException(
					"Got an unknown flag from " + "server: " + getFlags());
		}
		stringTable = readJavaScriptObject();
		typeTable = (JsArrayInteger) readJavaScriptObject();
	}

	public void doDeserialize(AsyncCallback postPrepareCallback) {
		this.postPrepareCallback = postPrepareCallback;
		Scheduler.get().scheduleIncremental(new AsyncDeserializer());
	}

	private AsyncCallback postPrepareCallback;

	enum Phase {
		INSTATIATE_EMPTY_SETUP, INSTATIATE_EMPTY_RUN,
		DESERIALIZE_NON_COLLECTION_PRE, DESERIALIZE_NON_COLLECTION_RUN,
		DESERIALIZE_COLLECTION_PRE, DESERIALIZE_COLLECTION_RUN
	}

	class AsyncDeserializer implements RepeatingCommand {
		private int typeTableLength;

		@Override
		public boolean execute() {
			try {
				String msg = phase + " - " + idx2 + " - "
						+ System.currentTimeMillis();
				consoleLog(msg);
				System.out.println(msg);
				switch (phase) {
				case INSTATIATE_EMPTY_SETUP:
					typeTableLength = typeTable.length();
					idx2 = 0;
					phase = Phase.INSTATIATE_EMPTY_RUN;
					// deliberate fallthrough
				case INSTATIATE_EMPTY_RUN:
					if (instantiateEmptyObjects()) {
						phase = Phase.DESERIALIZE_NON_COLLECTION_PRE;
					} else {
						break;
					}
				case DESERIALIZE_NON_COLLECTION_PRE:
					size = seenArray.size();
					int toss = readInt();// bypasss first object
					idx2 = 0;
					phase = Phase.DESERIALIZE_NON_COLLECTION_RUN;
					// deliberate fallthrough
				case DESERIALIZE_NON_COLLECTION_RUN:
					if (deserializeProperties()) {
						phase = Phase.DESERIALIZE_COLLECTION_PRE;
					} else {
						break;
					}
				case DESERIALIZE_COLLECTION_PRE:
					idx2 = 0;
					phase = Phase.DESERIALIZE_COLLECTION_RUN;
					// deliberate fallthrough
				case DESERIALIZE_COLLECTION_RUN:
					if (deserializeProperties()) {
						Scheduler.get().scheduleDeferred(
								new ScheduledCommand() {
									@Override
									public void execute() {
										postPrepareCallback.onSuccess(null);
									}
								});
						return false;
					} else {
						break;
					}
				}
			} catch (final Throwable e) {
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						postPrepareCallback.onFailure(e);
					}
				});
				return false;
			}
			return true;
		}

		private native void consoleLog(String s) /*-{
			$wnd.console.log(s);
		}-*/;

		private int sliceSize = 500;

		private int idx2;

		private Phase phase = Phase.INSTATIATE_EMPTY_SETUP;

		private int size;

		private boolean deserializeProperties() throws SerializationException {
			int sliceCount = sliceSize;
			for (; sliceCount != 0 && idx2 < size; idx2++) {
				Object instance = seenArray.get(idx2);
				boolean collectionOrMapNotFirst = idx2!=0&&(instance instanceof Collection || instance instanceof Map);
				if (collectionOrMapNotFirst
						^ (phase == Phase.DESERIALIZE_COLLECTION_RUN)) {
					continue;
				}
//				System.out.println(CommonUtils.simpleClassName(instance.getClass()));
				int strId = typeTable.get(idx2);
				String typeSignature = getString(strId);
				serializer.deserialize(ClientSerializationStreamReader.this,
						instance, typeSignature);
				sliceCount--;
			}
			return idx2 == size;
		}

		private boolean instantiateEmptyObjects() throws SerializationException {
			int sliceCount = sliceSize;
			for (; sliceCount != 0 && idx2 < typeTableLength; idx2++) {
				int strId = typeTable.get(idx2);
				String typeSignature = getString(strId);
				int id = reserveDecodedObjectIndex();
				Object instance = serializer.instantiate(
						ClientSerializationStreamReader.this, typeSignature);
				rememberDecodedObject(id, instance);
				sliceCount--;
			}
			return idx2 == typeTableLength;
		}
	}

	@Override
	public Object readObject() throws SerializationException {
		// at the end of the deserialization, return root
		if (index == 0) {
			return seenArray.get(0);
		}
		return super.readObject();
	}
//	public  boolean readBoolean(){
//		System.out.println("readBoolean");
//		return readBoolean0();
//	}
//	public native boolean readBoolean0() /*-{
//	return !!this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//public byte readByte(){
//System.out.println("readByte");
//return readByte0();
//}
//public native byte readByte0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//public char readChar(){
//System.out.println("readChar");
//return readChar0();
//}
//public native char readChar0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//public double readDouble(){
//System.out.println("readDouble");
//return readDouble0();
//}
//public native double readDouble0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//public float readFloat(){
//System.out.println("readFloat");
//return readFloat0();
//}
//public native float readFloat0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//public int readInt(){
//System.out.println("readInt");
//return readInt0();
//}
//public native int readInt0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
//
//@UnsafeNativeLong
//public long readLong(){
//System.out.println("readLong");
//return readLong0();
//}
//@UnsafeNativeLong
//public native long readLong0() /*-{
//	var s = this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//	return @com.google.gwt.lang.LongLib::longFromBase64(Ljava/lang/String;)(s);
//}-*/;
//
//public short readShort(){
//System.out.println("readShort");
//return readShort0();
//}
//public native short readShort0() /*-{
//	return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
//}-*/;
	
	
	
	public native boolean readBoolean() /*-{
		return !!this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	public native byte readByte() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	public native char readChar() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	public native double readDouble() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	public native float readFloat() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	public native int readInt() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

	@UnsafeNativeLong
	public native long readLong() /*-{
		var s = this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
		return @com.google.gwt.lang.LongLib::longFromBase64(Ljava/lang/String;)(s);
	}-*/;

	public native short readShort() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;

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
	protected native String getString(int index) /*-{
		// index is 1-based
		return index > 0 ? this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::stringTable[index - 1]
				: null;
	}-*/;

	private native JavaScriptObject readJavaScriptObject() /*-{
		return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
	}-*/;
}
