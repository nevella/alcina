package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.gwittir.validator.AsyncValidator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;

@Reflected
@Registration.Singleton
public class ReflectiveValidationRemoteServiceAsync
		extends ReflectiveRemoteServiceAsync {
	public static ReflectiveValidationRemoteServiceAsync get() {
		return Registry.impl(ReflectiveValidationRemoteServiceAsync.class);
	}

	public void validateAsync(Class<? extends AsyncValidator> validator,
			String value, AsyncCallback<ValidationResult> callback) {
		call("validateAsync", new Class[] { Class.class, String.class },
				callback, validator, value);
	}
}
