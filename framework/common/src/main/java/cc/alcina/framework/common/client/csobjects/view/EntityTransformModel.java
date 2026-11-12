package cc.alcina.framework.common.client.csobjects.view;

import cc.alcina.framework.common.client.serializer.TreeSerializable;

public interface EntityTransformModel extends TreeSerializable {
	default Object provideType() {
		return null;
	}
}
