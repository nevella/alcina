package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

/*
 * Either delta will be null, or all the other fields
 *  - delta non-null if we know the client will use it 
 *  immediately, saves an unnecessary double eval()
 */
public class DomainModelDeltaTransport implements Serializable, Cloneable {
	private String signature;

	private String metadataJson;

	private String serializedDelta;

	private DomainModelDelta delta;

	public DomainModelDeltaTransport() {
	}

	public DomainModelDeltaTransport(String signature, String metadataJson,
			String trancheString, DomainModelDelta delta) {
		this.signature = signature;
		this.metadataJson = metadataJson;
		this.serializedDelta = trancheString;
		this.delta = delta;
	}

	public DomainModelDeltaTransport clone() {
		DomainModelDeltaTransport o = new DomainModelDeltaTransport();
		o.signature = signature;
		o.metadataJson = metadataJson;
		o.serializedDelta = serializedDelta;
		o.delta = delta;
		return o;
	}

	public DomainModelDelta getDelta() {
		return this.delta;
	}

	public String getMetadataJson() {
		return this.metadataJson;
	}

	public String getSerializedDelta() {
		return this.serializedDelta;
	}

	public String getSignature() {
		return this.signature;
	}

	public boolean provideIsCacheReference() {
		return metadataJson == null && serializedDelta == null;
	}

	public void setDelta(DomainModelDelta delta) {
		this.delta = delta;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}

	public void setSerializedDelta(String serializedDelta) {
		this.serializedDelta = serializedDelta;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
}
