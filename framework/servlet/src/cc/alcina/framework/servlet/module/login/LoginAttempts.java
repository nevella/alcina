package cc.alcina.framework.servlet.module.login;

import java.util.Date;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.LoginAttempt;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class LoginAttempts {
	public boolean checkLockedOut(LoginModel loginModel) {
		List<LoginAttempt> attempts = (List) Domain.listByProperty(
				AlcinaPersistentEntityImpl.getImplementation(
						LoginAttempt.class),
				"userNameLowerCase",
				loginModel.loginBean.getUserName().toLowerCase());
		int maxAttempts = ResourceUtilities.getInteger(LoginAttempts.class,
				"maxAttempts");
		int maxAttemptsPeriodMins = ResourceUtilities
				.getInteger(LoginAttempts.class, "maxAttemptsPeriodMins");
		return attempts.stream()
				.sorted(Entity.EntityComparator.REVERSED_INSTANCE)
				.limit(maxAttempts).filter(a -> !a.isSuccess())
				.filter(a -> (System.currentTimeMillis()
						- a.getDate().getTime()) < maxAttemptsPeriodMins
								* TimeConstants.ONE_MINUTE_MS)
				.count() != maxAttempts;
	}

	protected boolean checkLockedOut() {
		return false;
	}

	protected void handleLoginResult(LoginModel loginModel) {
		int maxAttempts = ResourceUtilities.getInteger(LoginAttempts.class,
				"maxAttempts");
		Preconditions.checkArgument(maxAttempts != 0);
		List<LoginAttempt> attempts = (List) Domain.listByProperty(
				AlcinaPersistentEntityImpl.getImplementation(
						LoginAttempt.class),
				"userNameLowerCase",
				loginModel.loginBean.getUserName().toLowerCase());
		attempts.stream().sorted(Entity.EntityComparator.REVERSED_INSTANCE)
				.skip(maxAttempts).forEach(Entity::delete);
		LoginAttempt loginAttempt = Domain.create(AlcinaPersistentEntityImpl
				.getImplementation(LoginAttempt.class));
		loginAttempt.setUserNameLowerCase(
				loginModel.loginRequest.getUserName().toLowerCase());
		loginAttempt.setDate(new Date());
		loginAttempt.setIpAddress(ServletLayerUtils.robustGetRemoteAddress(
				CommonRemoteServiceServlet.getContextThreadLocalRequest()));
		loginAttempt.setSuccess(loginModel.loginResponse.isOk());
		loginAttempt.setUserAgent(ServletLayerUtils.getUserAgent(
				CommonRemoteServiceServlet.getContextThreadLocalRequest()));
		Transaction.commit();
	}
}
