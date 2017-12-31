package cc.alcina.framework.common.client.gwittir.validator;

import java.io.Serializable;

//typed server validation result to avoid two round trips
public interface ServerValidationResult extends Serializable {
	// to allow GWT compilation of ServerValidator, even if project has no
	// implementations
	public static class ServerValidationResultExample
			implements ServerValidationResult {
	}
}
