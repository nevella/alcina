package cc.alcina.framework.gwt.client.serializer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.GraphNode;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import elemental.json.Json;
import elemental.json.JsonValue;

@SuppressWarnings("deprecation")
public class ReflectiveSerializersGwt {
	public static class TypeSerializer_Value_GWT
			extends ReflectiveSerializer.TypeSerializer {
		@Override
		public boolean handlesDeclaredTypeSubclasses() {
			return true;
		}

		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(BasePlace.class);
		}

		@Override
		public boolean isReferenceSerializer() {
			return false;
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			return null;
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.serialNode.readValue(graphNode);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return incoming;
		}

		@Override
		public Iterator<ReflectiveSerializer.GraphNode>
				writeIterator(ReflectiveSerializer.GraphNode node) {
			return null;
		}

		@Override
		public void writeValueOrContainer(ReflectiveSerializer.GraphNode node,
				ReflectiveSerializer.SerialNode serialNode) {
			serialNode.write(node, node.value);
		}
	}

	public static class ValueSerializerBasePlace
			extends ReflectiveSerializer.ValueSerializer<BasePlace> {
		@Override
		protected BasePlace fromJson(Class clazz, JsonValue value) {
			return (BasePlace) RegistryHistoryMapper.get()
					.getPlaceOrThrow(value.asString());
		}

		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(BasePlace.class);
		}

		@Override
		public JsonValue toJsonValue(BasePlace object) {
			return Json.create(object.toTokenStringWithoutAppPrefix());
		}
	}
}
