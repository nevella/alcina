package cc.alcina.framework.entity.util;

import org.json.JSONObject;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.HasJsonRepresentation;

public class HasJsonRepresentationToJsonConverter implements
		Converter<HasJsonRepresentation, JSONObject> {
	@Override
	public JSONObject convert(HasJsonRepresentation original) {
		try {
			return original.asJson();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}