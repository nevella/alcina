/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.totsp.gwittir.client.beans.interfaces;

import com.totsp.gwittir.client.validator.Validator;

/**
 *
 * @author kebernet
 */
public interface SetValidatorRight {
	SetValidationFeedbackRight validateRightWith(Validator validator);

	SetValidationFeedbackRight validateRightWith(Validator... validators);
}
