package cc.alcina.framework.gwt.client.ide.provider;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

import com.totsp.gwittir.client.beans.Converter;

public class RegisterObjectsConverter implements Converter {
	@Override
	public Object convert(Object original) {
		if (original instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili1 = (HasIdAndLocalId) original;
			HasIdAndLocalId hili2 = TransformManager.get().getObject(hili1);
			if (hili2 != null) {
				return hili2;
			} else {
				TransformManager.get().registerDomainObject(hili1);
			}
		}
		return original;
	}
}
