package cc.alcina.framework.gwt.client.module.support.login;

import java.util.Collections;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.EnumPlayer;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.remote.ReflectiveLoginRemoteServiceAsync;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.module.support.login.LoginConsort.State;

@Reflected
public abstract class LoginConsort extends Consort<State> {
	protected LoginRequest request = new LoginRequest();

	protected Topic<Boolean> topicCallingRemote = Topic.create();

	protected Topic<String> topicMessage = Topic.create();

	private LoginResponse lastResponse;

	protected Consumer<Model> modelRenderer;

	public LoginConsort() {
	}

	public LoginResponse getLastResponse() {
		return lastResponse;
	}

	public String getPasswordPageSubtitleText() {
		return "Please enter your password";
	}

	public String getUsernamePageSubtitleText() {
		return "Enter your email to log in";
	}

	public void init(Consumer<Model> modelRenderer) {
		this.modelRenderer = modelRenderer;
		addPlayer(new Player_Got_username());
		addPlayer(new Player_Got_password());
		addPlayer(new Player_Got_2fa());
		addEndpointPlayer();
	}

	public boolean isRequiresValidEmail() {
		return true;
	}

	public void onClickNext() {
		/*
		 * TODO - remote action builder - promises
		 *
		 * FIXME - ui2 1x3 - debounce
		 */
		topicCallingRemote.publish(true);
		getAsyncService().login(request, new AsyncCallback<LoginResponse>() {
			@Override
			public void onFailure(Throwable caught) {
				topicCallingRemote.publish(false);
				String message = Ax.blankTo(caught.getMessage(),
						() -> CommonUtils.toSimpleExceptionMessage(caught));
				topicMessage.publish(message);
			}

			@Override
			public void onSuccess(LoginResponse loginResponse) {
				setLastResponse(loginResponse);
				topicCallingRemote.publish(false);
				handleSuccess(loginResponse);
			}
		});
	}

	public void setLastResponse(LoginResponse lastResponse) {
		this.lastResponse = lastResponse;
	}

	public boolean shouldShowQrCode() {
		return getLastResponse().getStates()
				.contains(LoginResponseState.Two_factor_qr_code_required);
	}

	protected abstract String getTitleText();

	protected void handleSuccess(LoginResponse response) {
		boolean hasRequestUsername = Ax.notBlank(request.getUserName());
		if (!hasRequestUsername) {
			clearReachedStates();
			nudge();
			//
			return;
		}
		if (Ax.notBlank(response.getErrorMsg())) {
			topicMessage.publish(response.getErrorMsg());
			return;
		}
		boolean hasRequestPassword = Ax.notBlank(request.getPassword());
		if (response.getStates()
				.contains(LoginResponseState.Username_not_found)) {
			handleUsernameNotFound();
			return;
		}
		addState(State.Got_username);
		if (!hasRequestPassword) {
			wasPlayed(playing.get(0));
			return;
		}
		if (response.getStates().contains(LoginResponseState.Password_incorrect)
				|| response.getStates()
						.contains(LoginResponseState.Invalid_credentials)) {
			return;
		}
		addState(State.Got_password);
		if (response.getStates()
				.contains(LoginResponseState.Two_factor_code_required)) {
			wasPlayed(playing.get(0));
			return;
		}
		if (response.getStates()
				.contains(LoginResponseState.Account_cannot_login)) {
			return;
		}
		wasPlayed(playing.get(0), Collections.singleton(State.Got_2fa_code));
	}

	protected void handleUsernameNotFound() {
		// for subclasses, e.g. show a 'sign up' dialog
	}

	ReflectiveLoginRemoteServiceAsync getAsyncService() {
		return Registry.impl(ReflectiveLoginRemoteServiceAsync.class);
	}

	public enum State {
		Got_username, Got_password, Got_2fa_code
	}

	class Player_Got_2fa extends EnumPlayer<State> {
		public Player_Got_2fa() {
			super(State.Got_2fa_code);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			modelRenderer.accept(new LoginPage2FA(LoginConsort.this));
		}
	}

	class Player_Got_password extends EnumPlayer<State> {
		public Player_Got_password() {
			super(State.Got_password);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			modelRenderer.accept(new LoginPagePassword(LoginConsort.this));
		}
	}

	class Player_Got_username extends EnumPlayer<State> {
		public Player_Got_username() {
			super(State.Got_username);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			modelRenderer.accept(new LoginPageUsername(LoginConsort.this));
		}
	}
}
