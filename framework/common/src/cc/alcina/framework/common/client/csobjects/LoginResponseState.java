package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public enum LoginResponseState {
	Username_not_found, Password_incorrect, Invalid_credentials,
	Two_factor_code_required, Two_factor_qr_code_required, Login_complete,
	Account_cannot_login, Unknown_exception, Account_locked_out, 
	Suggest_account_creation;
}
