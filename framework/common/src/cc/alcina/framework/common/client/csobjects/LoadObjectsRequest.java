package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * For very large object loads (to Africa), localdb-serializing on the client
 * and running transforms may be very nice
 * 
 * @author nick@alcina.cc
 * 
 */
public class LoadObjectsRequest implements Serializable {
	private String moduleTypeSignature;

	private DomainModelDeltaMetadata clientPersistedDomainObjectsMetadata;

	private Long userId;

	private List<String> clientDeltaSignatures = new ArrayList<String>();

	private List<DomainModelDeltaSignature> requestedModels = new ArrayList<DomainModelDeltaSignature>();

	private List<DomainModelDeltaSignature> requestedExactSignatures = new ArrayList<DomainModelDeltaSignature>();

	private Map<String, String> contextProperties = new LinkedHashMap<String, String>();

	public List<String> getClientDeltaSignatures() {
		return this.clientDeltaSignatures;
	}

	public DomainModelDeltaMetadata getClientPersistedDomainObjectsMetadata() {
		return this.clientPersistedDomainObjectsMetadata;
	}

	public Map<String, String> getContextProperties() {
		return this.contextProperties;
	}

	public String getModuleTypeSignature() {
		return this.moduleTypeSignature;
	}

	public List<DomainModelDeltaSignature> getRequestedExactSignatures() {
		return this.requestedExactSignatures;
	}

	public List<DomainModelDeltaSignature> getRequestedModels() {
		return this.requestedModels;
	}

	public Long getUserId() {
		return this.userId;
	}

	public void setClientDeltaSignatures(List<String> clientDeltaSignatures) {
		this.clientDeltaSignatures = clientDeltaSignatures;
	}

	public void setClientPersistedDomainObjectsMetadata(
			DomainModelDeltaMetadata domainObjectsMetadata) {
		this.clientPersistedDomainObjectsMetadata = domainObjectsMetadata;
	}

	public void setContextProperties(Map<String, String> contextProperties) {
		this.contextProperties = contextProperties;
	}

	public void setModuleTypeSignature(String modelDeltaRpcTypeSignature) {
		this.moduleTypeSignature = modelDeltaRpcTypeSignature;
	}

	public void setRequestedExactSignatures(
			List<DomainModelDeltaSignature> requestedExactSignatures) {
		this.requestedExactSignatures = requestedExactSignatures;
	}

	public void setRequestedModels(
			List<DomainModelDeltaSignature> requestedModels) {
		this.requestedModels = requestedModels;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Load models request - %s", requestedModels);
	}
}
