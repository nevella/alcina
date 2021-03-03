package cc.alcina.framework.common.client.view;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public interface EntityTransformModel {
	@ClientInstantiable
	public static abstract class Base extends Model
			implements EntityTransformModel {
	}
}
