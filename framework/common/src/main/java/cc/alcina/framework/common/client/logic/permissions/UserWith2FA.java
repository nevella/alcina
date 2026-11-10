package cc.alcina.framework.common.client.logic.permissions;

public interface UserWith2FA {
	public String getAuthenticationSecret();

	public boolean isHasSuccessfullyLoggedIn();

	public void setAuthenticationSecret(String authenticationSecret);

	public void setHasSuccessfullyLoggedIn(boolean hasSuccessfullyLoggedIn);
}
