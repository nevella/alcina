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
package cc.alcina.framework.common.client.logic.domaintransform;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import cc.alcina.framework.common.client.Reflections;

/**
 * Custom field serializer for EntityLocator
 */
@SuppressWarnings("unchecked")
public final class EntityLocator_CustomFieldSerializer
		extends CustomFieldSerializer<EntityLocator> {
	public static void deserialize(SerializationStreamReader streamReader,
			EntityLocator instance) throws SerializationException {
		instance.id=streamReader.readLong();
		instance.localId=streamReader.readLong();
		instance.clazz=Reflections.classLookup().getClassForName(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter,
			EntityLocator instance) throws SerializationException {
		streamWriter.writeLong(instance.id);
		streamWriter.writeLong(instance.localId);
		streamWriter.writeString(instance.clazz.getName());
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader,
			EntityLocator instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter,
			EntityLocator instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
