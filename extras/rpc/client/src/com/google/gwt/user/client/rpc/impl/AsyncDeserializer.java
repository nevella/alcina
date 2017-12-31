package com.google.gwt.user.client.rpc.impl;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.SerializationException;

import cc.alcina.framework.common.client.util.MultikeyMap;

class AsyncDeserializer implements RepeatingCommand {
	private int typeTableLength;

	private final ClientSerializationStreamReader reader;

	private int sliceSize = 500;

	private int idx2;

	private Phase phase = Phase.INSTATIATE_EMPTY_SETUP;

	private int size;

	public AsyncDeserializer(ClientSerializationStreamReader reader) {
		this.reader = reader;
	}

	@Override
	public boolean execute() {
		try {
			// String msg = phase + " - " + idx2 + " - "
			// + System.currentTimeMillis();
			// consoleLog(msg);
			// System.out.println(msg);
			switch (phase) {
			case INSTATIATE_EMPTY_SETUP:
				typeTableLength = reader.getTypeTableLength();
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
				size = reader.seenArray.size();
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
					Scheduler.get().scheduleDeferred(new ScheduledCommand() {
						@Override
						public void execute() {
							reader.postPrepareCallback.onSuccess(null);
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
					reader.postPrepareCallback.onFailure(e);
				}
			});
			return false;
		}
		return true;
	}

	private void consoleLog(String s) {
	}

	private native void consoleLogOld(String s) /*-{
												$wnd.console.log(s);
												}-*/;

	private boolean deserializeProperties() throws SerializationException {
		int sliceCount = sliceSize;
		for (; sliceCount != 0 && idx2 < size; idx2++) {
			Object instance = reader.seenArray.get(idx2);
			// keep in sync with asyncdeserializer, clientserreader,
			// serverserwriter
			boolean collectionOrMap = instance instanceof Collection
					|| instance instanceof Map
					|| instance instanceof MultikeyMap;
			if (collectionOrMap ^ (phase == Phase.DESERIALIZE_COLLECTION_RUN)) {
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

	private boolean instantiateEmptyObjects() throws SerializationException {
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

	enum Phase {
		INSTATIATE_EMPTY_SETUP, INSTATIATE_EMPTY_RUN,
		DESERIALIZE_NON_COLLECTION_PRE, DESERIALIZE_NON_COLLECTION_RUN,
		DESERIALIZE_COLLECTION_PRE, DESERIALIZE_COLLECTION_RUN
	}
}