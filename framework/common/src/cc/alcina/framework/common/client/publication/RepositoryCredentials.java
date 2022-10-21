package cc.alcina.framework.common.client.publication;

public class RepositoryCredentials {
	private String principalId;

	private String principalSecret;

	private String repositoryPath;

	public String getPrincipalId() {
		return principalId;
	}

	public String getPrincipalSecret() {
		return principalSecret;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}
	
	public void setPrincipalId(String principalId) {
		this.principalId = principalId;
	}

	public void setPrincipalSecret(String principalSecret) {
		this.principalSecret = principalSecret;
	}

	public void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
	}
}
