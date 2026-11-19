/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.beans.interfaces;

import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.Validator;

/**
 *
 * @author kebernet
 */
public interface SetValidatorLeft {
	SetValidationFeedbackLeft validateLeftWith(Validator validator);

	SetValidationFeedbackLeft validateLeftWith(Validator... validators);
}
