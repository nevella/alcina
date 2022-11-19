package cc.alcina.framework.common.client.repository;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@ReflectiveSerializer.Checks(ignore = true)
public class RepositoryConnection extends Bindable implements TreeSerializable {
	private String principalId;

	private String principalSecret;

	private String region;

	private String repositoryPath;

	@GwtTransient
	/**
	 * Note that the corresponding property should *not* be AlcinaTransient -
	 * omitting cos GWT non-refl ser doesn't support class
	 *
	 * FIXME - dirndl 1x1f - remove transient
	 * ann, @ReflectiveSerializer.Checks(ignore = true)
	 */
	private Class<? extends RepositoryType> type;

	public String getPrincipalId() {
		return principalId;
	}

	public String getPrincipalSecret() {
		return principalSecret;
	}

	public String getRegion() {
		return this.region;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}

	public Class<? extends RepositoryType> getType() {
		return this.type;
	}

	public void setPrincipalId(String principalId) {
		this.principalId = principalId;
	}

	public void setPrincipalSecret(String principalSecret) {
		this.principalSecret = principalSecret;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
	}

	public void setType(Class<? extends RepositoryType> type) {
		this.type = type;
	}
}
