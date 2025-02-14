package cc.alcina.framework.entity.persistence.mvcc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.KryoUtils.EntitySerializer;

public class KryoSupport {
	public static final String CONTEXT_DESERIALIZING_PRODUCTION_GRAPH = KryoSupport.class
			.getName() + ".CONTEXT_DESERIALIZING_PRODUCTION_GRAPH";

	/*
	 * IMPORTANT - always use in conjunction with
	 * LooseContext.setTrue(KryoUtils.CONTEXT_BYPASS_POOL), otherwise may be
	 * missed due to kryo caching
	 */
	public static final String CONTEXT_FORCE_ENTITY_SERIALIZER = KryoSupport.class
			.getName() + ".CONTEXT_FORCE_ENTITY_SERIALIZER";

	@Registration.Singleton(SerializerFactory.class)
	public static class MvccInterceptorSerializer implements SerializerFactory {
		@Override
		public Serializer makeSerializer(Kryo kryo, Class<?> type) {
			if (MvccObject.class.isAssignableFrom(type)) {
				return new MvccObjectSerializer(kryo, type);
			}
			if (Entity.class.isAssignableFrom(type)) {
				// this could go to production, but it's mostly needed for
				// console/webapp comms - since they have circular graphs where
				// id/localid affects the hash
				//
				// FIXME - Kryo - make it production with kryo 7 (or whatever)
				//
				// in the meantime, set the context variable if deserializing a
				// production graph
				boolean useEntitySerializer = !LooseContext
						.is(CONTEXT_DESERIALIZING_PRODUCTION_GRAPH);
				// allow production code to force this serializer (to, for
				// instance, not copy transient fields)
				useEntitySerializer |= LooseContext
						.is(CONTEXT_FORCE_ENTITY_SERIALIZER);
				if (useEntitySerializer) {
					return new EntitySerializer(kryo, type);
				}
			}
			return new FieldSerializer<>(kryo, type);
		}
	}

	public static class MvccObjectSerializer extends Serializer {
		public MvccObjectSerializer(Kryo kryo, Class<?> type) {
			setImmutable(true);
		}

		@Override
		public Object read(Kryo kryo, Input input, Class type) {
			return Domain.find(Mvcc.resolveEntityClass(type), input.readLong());
		}

		@Override
		public void write(Kryo kryo, Output output, Object object) {
			Entity entity = (Entity) object;
			output.writeLong(entity.getId());
		}
	}
}
