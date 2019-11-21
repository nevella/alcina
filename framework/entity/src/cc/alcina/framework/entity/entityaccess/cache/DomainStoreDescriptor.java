package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.reflect.Field;
import java.util.List;

import com.google.gwt.dev.util.collect.IdentityHashMap;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.Counter;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.ObjectMemory;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.projection.GraphProjection;

public abstract class DomainStoreDescriptor extends DomainDescriptor
		implements MemoryStatProvider {
	protected DomainSegmentLoader domainSegmentLoader;

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent, StatType type) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		perClass.values()
				.forEach(descriptor -> descriptor.addMemoryStats(self, type));
		return self;
	}

	public DomainSegmentLoader getDomainSegmentLoader() {
		return domainSegmentLoader;
	}

	public abstract Class<? extends DomainTransformRequestPersistent>
			getDomainTransformRequestPersistentClass();

	public Class<? extends ClassRef> getShadowClassRefClass() {
		throw new UnsupportedOperationException();
	}

	public Class<? extends DomainTransformEvent>
			getShadowDomainTransformEventPersistentClass() {
		throw new UnsupportedOperationException();
	}

	public boolean isAddMvccObjectResolutionChecks() {
		return false;
	}

	public boolean isUseTransformDbCommitSequencing() {
		return true;
	}

	public void saveSegmentData() {
		throw new UnsupportedOperationException();
	}

	public static class ObjectMemoryImpl extends ObjectMemory {
		GraphProjection projection = new GraphProjection();

		IdentityHashMap seen = new IdentityHashMap<>();

		@Override
		public void walkStats(Object o, Counter counter) {
			if (o == null) {
				return;
			}
			Class<? extends Object> clazz = o.getClass();
			/*
			 * guaranteed not primitive
			 */
			if (seen.put(o, o) != null) {
				return;
			}
			counter.count++;
			long size = getObjectSize(o);
			counter.size += size;
			counter.perClass.merge(clazz, size, (v1, v2) -> v1 + v2);
			try {
				if (clazz.isArray()) {
					Class<?> componentType = clazz.getComponentType();
					if (componentType.isPrimitive()) {
						return;
					} else {
						Object[] array = (Object[]) o;
						for (int idx = 0; idx < array.length; idx++) {
							walkStats(o, counter);
						}
					}
				} else {
					List<Field> fields = projection.getFieldsForClass(clazz);
					for (Field field : fields) {
						if (field.getType().isPrimitive()) {
							// done, allocated in container
							return;
						} else {
							Object value = field.get(o);
							walkStats(value, counter);
						}
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private long getObjectSize(Object o) {
			if (o == null) {
				return 0;
			}
			return jdk.nashorn.internal.ir.debug.ObjectSizeCalculator
					.getObjectSize(o);
		}
	}
}
