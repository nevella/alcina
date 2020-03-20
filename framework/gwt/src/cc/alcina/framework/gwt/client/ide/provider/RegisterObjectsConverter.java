package cc.alcina.framework.gwt.client.ide.provider;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

public class RegisterObjectsConverter implements Converter {
	@Override
	public Object convert(Object original) {
		if (original instanceof Entity) {
			Entity entity1 = (Entity) original;
			Entity entity2 = TransformManager.get().getObject(entity1);
			if (entity2 != null) {
				return entity2;
			} else {
				TransformManager.get().registerDomainObject(entity1);
			}
		}
		return original;
	}
}
