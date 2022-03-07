package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
public class DomainModelDeltaMetadata implements Serializable {
	private Long maxPersistedTransformIdWhenGenerated;

	private Date generationDate;

	private Long userId;

	private String contentObjectRpcTypeSignature;

	private String contentObjectClassName;

	private boolean domainObjectsFieldSet;

	public String getContentObjectClassName() {
		return this.contentObjectClassName;
	}

	public String getContentObjectRpcTypeSignature() {
		return this.contentObjectRpcTypeSignature;
	}

	public Date getGenerationDate() {
		return this.generationDate;
	}

	public Long getMaxPersistedTransformIdWhenGenerated() {
		return this.maxPersistedTransformIdWhenGenerated;
	}

	public Long getUserId() {
		return this.userId;
	}

	public boolean isDomainObjectsFieldSet() {
		return this.domainObjectsFieldSet;
	}

	public void setContentObjectClassName(String contentObjectClassName) {
		this.contentObjectClassName = contentObjectClassName;
	}

	public void setContentObjectRpcTypeSignature(
			String contentObjectRpcTypeSignature) {
		this.contentObjectRpcTypeSignature = contentObjectRpcTypeSignature;
	}

	public void setDomainObjectsFieldSet(boolean domainObjectsFieldSet) {
		this.domainObjectsFieldSet = domainObjectsFieldSet;
	}

	public void setGenerationDate(Date generationDate) {
		this.generationDate = generationDate;
	}

	public void setMaxPersistedTransformIdWhenGenerated(
			Long maxPersistedTransformIdWhenGenerated) {
		this.maxPersistedTransformIdWhenGenerated = maxPersistedTransformIdWhenGenerated;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}
