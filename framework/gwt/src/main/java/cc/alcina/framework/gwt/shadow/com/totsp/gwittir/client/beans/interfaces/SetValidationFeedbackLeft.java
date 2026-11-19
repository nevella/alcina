/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.beans.interfaces;

import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.ValidationFeedback;

/**
 *
 * @author kebernet
 */
public interface SetValidationFeedbackLeft {
	SetRight notifiedWithLeft(ValidationFeedback feedback);

	SetRight notifiedWithLeft(ValidationFeedback... feedbacks);
}
