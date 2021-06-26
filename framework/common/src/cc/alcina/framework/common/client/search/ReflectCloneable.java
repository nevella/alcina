package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.util.CloneHelper;

public interface ReflectCloneable<T extends ReflectCloneable> {
	default <TT extends T> TT cloneObject() {
		try {
			return (TT) FlatTreeSerializer.clone((TreeSerializable) this);
		} catch (Exception e) {
			try {
				return (TT) new CloneHelper().deepBeanClone(this);
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		}
	}
}
