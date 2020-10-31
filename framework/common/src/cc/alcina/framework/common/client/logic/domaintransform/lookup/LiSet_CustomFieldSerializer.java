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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Collection_CustomFieldSerializerBase;

/**
 * Custom field serializer for LiSet
 */
public final class LiSet_CustomFieldSerializer
		extends CustomFieldSerializer<LiSet> {
	public static transient boolean incorrectHashes=false;
	private static List<LiSet> clientDeserialized;
	public static List<LiSet> getClientDeserialized() {
		return LiSet_CustomFieldSerializer.clientDeserialized;
	}

	public static void deserialize(SerializationStreamReader streamReader,
			LiSet instance) throws SerializationException {
		Collection_CustomFieldSerializerBase.deserialize(streamReader,
				instance);
		if(GWT.isClient()&&incorrectHashes) {
			if(clientDeserialized==null) {
				clientDeserialized=new ArrayList<>();
			}
			clientDeserialized.add(instance);
		}
	}

	public static void serialize(SerializationStreamWriter streamWriter,
			LiSet instance) throws SerializationException {
		Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
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

	public static void rehashLiSets() {
		if(clientDeserialized==null) {
			clientDeserialized.forEach(LiSet::reHash);
			clientDeserialized.clear();
		}
	}
}
