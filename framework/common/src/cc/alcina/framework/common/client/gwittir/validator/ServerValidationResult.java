package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

// typed server validation result to avoid two round trips
//
@Bean
public abstract class ServerValidationResult {
	// to allow GWT compilation of ServerValidator, even if project has no
	// implementations
	public static class ServerValidationResultExample
			extends ServerValidationResult {
	}
}
