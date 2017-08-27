package cc.alcina.framework.common.client.logic.domaintransform;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class HiliLocatorConverter implements Converter<HasIdAndLocalId,HiliLocator>{

	@Override
	public HiliLocator convert(HasIdAndLocalId original) {
		return new HiliLocator(original);
	}
	
}