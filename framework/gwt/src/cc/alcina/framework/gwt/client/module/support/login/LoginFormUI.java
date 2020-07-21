package cc.alcina.framework.gwt.client.module.support.login;

import com.totsp.gwittir.client.validator.ValidationFeedback;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.gwittir.widget.GridFormCellRenderer;
import cc.alcina.framework.gwt.client.lux.LuxFormCellRenderer;
import cc.alcina.framework.gwt.client.lux.LuxStyleValidationFeedback;

@ClientInstantiable
@RegistryLocation(registryPoint = LoginFormUI.class, implementationType = ImplementationType.INSTANCE)
public class LoginFormUI {
	public GridFormCellRenderer getRenderer() {
		return new LuxFormCellRenderer();
	}

	public ValidationFeedback getValidationFeedback() {
		return new LuxStyleValidationFeedback("validation-error");
	}
}