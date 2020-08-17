package cc.alcina.framework.entity.logic;

import cc.alcina.framework.entity.entityaccess.WrappedObject;

public interface EntityLayerTransformPropagation {
	boolean listenToWrappedObject(WrappedObject wrapper);
}
