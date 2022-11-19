package cc.alcina.framework.common.client.repository;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

public class RepositoryConnection extends Bindable implements TreeSerializable {
	private String principalId;

	private String principalSecret;

	private String region;

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
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

	private String repositoryPath;

	private Class<? extends RepositoryType> type;

	public Class<? extends RepositoryType> getType() {
		return this.type;
	}

	public void setType(Class<? extends RepositoryType> type) {
		this.type = type;
	}

	public String getPrincipalId() {
		return principalId;
	}

	public String getPrincipalSecret() {
		return principalSecret;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}
}
