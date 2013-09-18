package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;

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
	
	private List<String> clientDeltaSignatures=new ArrayList<String>();
	
	public List<String> getClientDeltaSignatures() {
		return this.clientDeltaSignatures;
	}

	public DomainModelDeltaMetadata getClientPersistedDomainObjectsMetadata() {
		return this.clientPersistedDomainObjectsMetadata;
	}

	public String getModuleTypeSignature() {
		return this.moduleTypeSignature;
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

	public void setModuleTypeSignature(String modelDeltaRpcTypeSignature) {
		this.moduleTypeSignature = modelDeltaRpcTypeSignature;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}
