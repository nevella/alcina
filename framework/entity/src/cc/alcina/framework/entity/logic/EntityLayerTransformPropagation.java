package cc.alcina.framework.entity.logic;

import cc.alcina.framework.entity.persistence.WrappedObject;

public interface EntityLayerTransformPropagation {
	boolean listenToWrappedObject(WrappedObject wrapper);
}
