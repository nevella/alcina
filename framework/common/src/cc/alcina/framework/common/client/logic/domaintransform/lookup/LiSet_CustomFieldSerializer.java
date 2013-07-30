/*
 * Copyright 2007 Google Inc.
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
package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for LiSet
 */
@SuppressWarnings("unchecked")
public final class LiSet_CustomFieldSerializer extends
		CustomFieldSerializer<LiSet> {
	public static void deserialize(SerializationStreamReader streamReader,
			LiSet instance) throws SerializationException {
		int size = streamReader.readInt();
		for (int i = 0; i < size; ++i) {
			Object obj = streamReader.readObject();
			instance.add(obj);
		}
	}

	public static void serialize(SerializationStreamWriter streamWriter,
			LiSet instance) throws SerializationException {
		int size = instance.size();
		streamWriter.writeInt(size);
		for (Object obj : instance) {
			streamWriter.writeObject(obj);
		}
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader,
			LiSet instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter,
			LiSet instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
