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
package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for MultikeyMapBase *Important* - does not ensure
 * presence of map items/keys. Must be ensured by programmer
 */
@SuppressWarnings("rawtypes")
public final class MultikeyMapBase_CustomFieldSerializer {
	public static void deserialize(SerializationStreamReader streamReader,
			MultikeyMapBase instance) throws SerializationException {
		instance.setDepth(streamReader.readInt());
		int tupleCount = streamReader.readInt();
		for (int idx0 = 0; idx0 < tupleCount; idx0++) {
			int fieldCount = streamReader.readInt();
			List tuple = new ArrayList();
			for (int idx1 = 0; idx1 < fieldCount; idx1++) {
				tuple.add(streamReader.readObject());
			}
			instance.addTuples(Collections.singletonList(tuple));
		}
	}

	public static void serialize(SerializationStreamWriter streamWriter,
			MultikeyMapBase instance) throws SerializationException {
		streamWriter.writeInt(instance.getDepth());
		List<List> tuples = instance.asTuples(instance.getDepth());
		streamWriter.writeInt(tuples.size());
		for (List tuple : tuples) {
			streamWriter.writeInt(tuple.size());
			for (Object object : tuple) {
				streamWriter.writeObject(object);
			}
		}
	}
}
