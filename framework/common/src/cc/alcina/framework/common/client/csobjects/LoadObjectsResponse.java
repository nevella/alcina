package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaTransport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObject;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTranche;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
public class LoadObjectsResponse implements Serializable {
	private LoadObjectsRequest request;

	private String appInstruction;

	private List<String> preserveClientDeltaSignatures = new ArrayList<String>();

	private List<DomainModelDeltaTransport> deltaTransports = new ArrayList<DomainModelDeltaTransport>();

	private List<DomainModelDeltaTransport> loadSequenceTransports = new ArrayList<DomainModelDeltaTransport>();

	public String getAppInstruction() {
		return this.appInstruction;
	}

	public List<DomainModelDeltaTransport> getDeltaTransports() {
		return this.deltaTransports;
	}

	public List<DomainModelDeltaTransport> getLoadSequenceTransports() {
		return this.loadSequenceTransports;
	}

	public List<String> getPreserveClientDeltaSignatures() {
		return this.preserveClientDeltaSignatures;
	}

	public LoadObjectsRequest getRequest() {
		return request;
	}

	public <T extends DomainModelObject> T
			provideDomainModelObject(Class<T> clazz) {
		return (T) getDeltaTransports().stream()
				.map(DomainModelDeltaTransport::getDelta)
				.map(DomainModelDelta::getDomainModelObject)
				.filter(Objects::nonNull).filter(dmo -> dmo.getClass() == clazz)
				.findFirst().get();
	}

	public void putDomainModelHolder(DomainModelHolder domainModelHolder) {
		DomainTranche tranche = new DomainTranche();
		tranche.setDomainModelHolder(domainModelHolder);
		DomainModelDeltaTransport transport = new DomainModelDeltaTransport();
		transport.setDelta(tranche);
		deltaTransports.add(transport);
		loadSequenceTransports.add(transport);
		// for non-persistent systems, so no signature checks
		// preserveClientDeltaSignatures.add(transport.getSignature());
	}

	public void setAppInstruction(String appInstruction) {
		this.appInstruction = appInstruction;
	}

	public void setDeltaTransports(
			List<DomainModelDeltaTransport> deltaTransports) {
		this.deltaTransports = deltaTransports;
	}

	public void setLoadSequenceTransports(
			List<DomainModelDeltaTransport> loadSequenceTransports) {
		this.loadSequenceTransports = loadSequenceTransports;
	}

	public void setPreserveClientDeltaSignatures(
			List<String> preserveClientDeltaSignatures) {
		this.preserveClientDeltaSignatures = preserveClientDeltaSignatures;
	}

	public void setRequest(LoadObjectsRequest request) {
		this.request = request;
	}
}
