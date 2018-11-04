package cc.alcina.framework.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

public class LiSetSerializer extends CollectionSerializer {
	private boolean elementsCanBeNull = true;

	private Serializer serializer;

	private Class elementClass;

	private Class genericType;

	Map<Collection, List> perSetElements = new IdentityHashMap();

	public void deserializationFinished() {
		// NOW the hashes'll be ok
		perSetElements.forEach((collection, list) -> {
			list.forEach(collection::add);
		});
	}

	@Override
	public Collection read(Kryo kryo, Input input, Class<Collection> type) {
		Collection collection = create(kryo, input, type);
		ArrayList<Object> list = new ArrayList<>();
		perSetElements.put(collection, list);
		kryo.reference(collection);
		int length = input.readVarInt(true);
		if (collection instanceof ArrayList)
			((ArrayList) collection).ensureCapacity(length);
		Class elementClass = this.elementClass;
		Serializer serializer = this.serializer;
		if (genericType != null) {
			if (serializer == null) {
				elementClass = genericType;
				serializer = kryo.getSerializer(genericType);
			}
			genericType = null;
		}
		if (serializer != null) {
			if (elementsCanBeNull) {
				for (int i = 0; i < length; i++)
					collection.add(kryo.readObjectOrNull(input, elementClass,
							serializer));
			} else {
				for (int i = 0; i < length; i++)
					collection.add(
							kryo.readObject(input, elementClass, serializer));
			}
		} else {
			for (int i = 0; i < length; i++) {
				list.add(kryo.readClassAndObject(input));
			}
		}
		return collection;
	}
}