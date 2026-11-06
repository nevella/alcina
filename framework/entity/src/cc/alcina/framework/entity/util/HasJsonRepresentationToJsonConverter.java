package cc.alcina.framework.entity.util;

import org.json.JSONObject;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.HasJsonRepresentation;

public class HasJsonRepresentationToJsonConverter
		implements Function<HasJsonRepresentation, JSONObject> {
	@Override
	public JSONObject apply(HasJsonRepresentation original) {
		try {
			return original.asJson();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}