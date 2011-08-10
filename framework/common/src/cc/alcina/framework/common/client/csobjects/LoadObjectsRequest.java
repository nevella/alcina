package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;

/**
 * For very large object loads (to Africa), localdb-serializing on the client
 * and running transforms may be very nice
 * 
 * @author nick@alcina.cc
 * 
 */
public class LoadObjectsRequest implements Serializable {
	private Long lastTransformId;

	private String typeSignature;
	
	

	public Long getLastTransformId() {
		return this.lastTransformId;
	}

	public String getTypeSignature() {
		return this.typeSignature;
	}

	

	public void setLastTransformId(Long lastTransformId) {
		this.lastTransformId = lastTransformId;
	}

	public void setTypeSignature(String typeSignature) {
		this.typeSignature = typeSignature;
	}
}
