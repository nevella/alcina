package cc.alcina.framework.common.client.csobjects.view;

import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public interface EntityTransformModel extends TreeSerializable {
	default Object provideType() {
		return null;
	}

	public static abstract class BaseModel extends Model
			implements EntityTransformModel {
	}
}
