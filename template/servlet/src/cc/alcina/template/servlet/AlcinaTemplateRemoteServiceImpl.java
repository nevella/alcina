package cc.alcina.template.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteService;
import cc.alcina.template.entityaccess.AlcinaTemplateBeanProvider;

public class AlcinaTemplateRemoteServiceImpl extends CommonRemoteServiceServlet
		implements AlcinaTemplateRemoteService {
	public AlcinaTemplateRemoteServiceImpl() {
		super();
		setLogger(Logger
				.getLogger(AlcinaServerConfig.get().getMainLoggerName()));
	}

	@Override
	public LoadObjectsResponse loadInitial(LoadObjectsRequest request) {
		AlcinaTemplateObjects alcinaTemplateObjects = AlcinaTemplateServerManager
				.get()
				.loadInitial(
						PermissionsManager
								.get()
								.isMemberOfGroup(
										AlcinaTemplateAccessConstants.ADMINISTRATORS_GROUP_NAME));
		HttpServletRequest req = getThreadLocalRequest();
		alcinaTemplateObjects.setOnetimeMessage((String) req.getSession()
				.getAttribute(SessionHelper.SESSION_ATTR_ONE_TIME_STRING));
		LoadObjectsResponse results = new LoadObjectsResponse();
		results.putDomainModelHolder(alcinaTemplateObjects);
		return results;
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		try {
			return super.search(def, pageNumber);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public LoginResponse login(LoginBean loginBean) {
		LoginResponse lrb = new LoginResponse();
		try {
			lrb = new Authenticator().authenticate(loginBean);
			if (lrb.isOk()) {
				processValidLogin(lrb, loginBean.getUserName());
				lrb.setClientInstance(AlcinaTemplateBeanProvider.get()
						.getCommonPersistenceBean()
						.createClientInstance(getUserAgent()));
				new CookieHelper().setRememberMeCookie(getThreadLocalRequest(),
						getThreadLocalResponse(), loginBean.isRememberMe());
			}
		} catch (AuthenticationException e) {
			lrb.setOk(false);
			String err = e.getMessage();
			if (e.getMessage().equals("Invalid username")) {
				err = "Email address not registered";
			}
			lrb.setErrorMsg(err);
			getLogger().warn("Login exception", e);
		}
		if (!lrb.isOk()) {
			EntityLayerUtils.log(LogMessageType.INVALID_AUTHENTICATION, String
					.format("Invalid login: %s (password obscured)",
							loginBean.getUserName()));
		} else {
		}
		return lrb;
	}

	@Override
	protected void processValidLogin(LoginResponse lrb, String userName)
			throws AuthenticationException {
		AlcinaTemplateUser user = new Authenticator()
				.processAuthenticatedLogin(lrb, userName);
		if (lrb.isOk()) {
			Registry.impl(SessionHelper.class).setupSessionForUser(
					getThreadLocalRequest(), getThreadLocalResponse(), user);
			lrb.setFriendlyName(user.getFirstName() + " " + user.getLastName());
		}
	}

	public LoginResponse hello() {
		String userName = null;
		if (PermissionsManager.get().isLoggedIn()) {// by sessionhelper
			userName = PermissionsManager.get().getUserName();
		}
		LoginResponse lrb = new LoginResponse();
		if (userName != null) {
			lrb.setOk(true);
			try {
				processValidLogin(lrb, userName);
			} catch (AuthenticationException e) {
				getLogger().warn("Hello exception", e);
			}
		}
		lrb.setClientInstance(AlcinaTemplateBeanProvider.get()
				.getCommonPersistenceBean()
				.createClientInstance(getUserAgent()));
		return lrb;
	}

	public void logout() {
		new CookieHelper().clearRemembermeCookie(getThreadLocalRequest(),
				getThreadLocalResponse());
		Registry.impl(SessionHelper.class).resetSession(
				getThreadLocalRequest(), getThreadLocalResponse());
	}

	@Override
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformRequestException {
		try {
			return super.transform(request);
		} catch (DomainTransformRequestException dte) {
			getLogger().warn(
					String.format("Transform exception: %s %s",
							PermissionsManager.get().getUserName(), request),
					dte);
			EntityLayerUtils.log(LogMessageType.TRANSFORM_EXCEPTION, String
					.format("Transform exception: %s %s", PermissionsManager
							.get().getUserName(), request), dte);
			throw dte;
		}
	}

	public List<AlcinaTemplateGroup> getAllGroups() {
		return AlcinaTemplateBeanProvider.get()
				.getAlcinaTemplatePersistenceBean().getAllGroups();
	}
}
