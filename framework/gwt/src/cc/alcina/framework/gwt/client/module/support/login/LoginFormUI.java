package cc.alcina.framework.gwt.client.module.support.login;

import com.totsp.gwittir.client.validator.ValidationFeedback;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.gwittir.widget.GridFormCellRenderer;
import cc.alcina.framework.gwt.client.lux.LuxFormCellRenderer;
import cc.alcina.framework.gwt.client.lux.LuxStyleValidationFeedback;

@Reflected
@Registration(LoginFormUI.class)
public class LoginFormUI {
	public GridFormCellRenderer getRenderer() {
		return new LuxFormCellRenderer();
	}

	public ValidationFeedback getValidationFeedback() {
		return new LuxStyleValidationFeedback("validation-error");
	}
}
