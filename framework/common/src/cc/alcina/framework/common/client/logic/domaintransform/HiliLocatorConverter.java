package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.totsp.gwittir.client.beans.Converter;

public class HiliLocatorConverter implements Converter<HasIdAndLocalId,HiliLocator>{

	@Override
	public HiliLocator convert(HasIdAndLocalId original) {
		return new HiliLocator(original);
	}
	
}