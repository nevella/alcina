package cc.alcina.framework.common.client.logic.permissions;

public interface UserWith2FA {
	public boolean isHasSuccessfullyLoggedIn();
	public void setHasSuccessfullyLoggedIn(boolean hasSuccessfullyLoggedIn);
	
	public String getAuthenticationSecret();
	public void setAuthenticationSecret(String authenticationSecret);
}
