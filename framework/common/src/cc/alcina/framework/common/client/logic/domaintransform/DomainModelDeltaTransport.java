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

	public DomainModelDeltaTransport clone() {
		DomainModelDeltaTransport o = new DomainModelDeltaTransport();
		o.signature = signature;
		o.metadataJson = metadataJson;
		o.serializedDelta = serializedDelta;
		o.delta = delta;
		return o;
	}

	public DomainModelDeltaTransport(String signature, String metadataJson,
			String trancheString, DomainModelDelta delta) {
		this.signature = signature;
		this.metadataJson = metadataJson;
		this.serializedDelta = trancheString;
		this.delta = delta;
	}

	public String getSignature() {
		return this.signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getMetadataJson() {
		return this.metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}

	public String getSerializedDelta() {
		return this.serializedDelta;
	}

	public void setSerializedDelta(String serializedDelta) {
		this.serializedDelta = serializedDelta;
	}

	public DomainModelDelta getDelta() {
		return this.delta;
	}

	public void setDelta(DomainModelDelta delta) {
		this.delta = delta;
	}

	public boolean provideIsCacheReference() {
		return metadataJson == null && serializedDelta == null;
	}
}
