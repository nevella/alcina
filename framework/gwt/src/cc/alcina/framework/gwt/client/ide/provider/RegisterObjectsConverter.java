package cc.alcina.framework.gwt.client.ide.provider;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

public class RegisterObjectsConverter implements Converter {
	@Override
	public Object convert(Object original) {
		if (original instanceof Entity) {
			Entity hili1 = (Entity) original;
			Entity hili2 = TransformManager.get().getObject(hili1);
			if (hili2 != null) {
				return hili2;
			} else {
				TransformManager.get().registerDomainObject(hili1);
			}
		}
		return original;
	}
}
