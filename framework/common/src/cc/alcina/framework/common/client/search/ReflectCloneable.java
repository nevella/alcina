package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CloneHelper;

public interface ReflectCloneable<T extends ReflectCloneable> {
	default <TT extends T> TT cloneObject() {
		try {
			return (TT) new CloneHelper().deepBeanClone(this);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
