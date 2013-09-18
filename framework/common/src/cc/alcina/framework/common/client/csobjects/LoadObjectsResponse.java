package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaTransport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTranche;

public class LoadObjectsResponse implements Serializable {
	private LoadObjectsRequest request;

	private String appInstruction;

	private List<String> preserveClientDeltaSignatures=new ArrayList<String>();
	
	private List<DomainModelDeltaTransport> deltaTransports=new ArrayList<DomainModelDeltaTransport>();
	
	private List<DomainModelDeltaTransport> loadSequenceTransports=new ArrayList<DomainModelDeltaTransport>();

	public String getAppInstruction() {
		return this.appInstruction;
	}

	public List<DomainModelDeltaTransport> getDeltaTransports() {
		return this.deltaTransports;
	}

	public List<String> getPreserveClientDeltaSignatures() {
		return this.preserveClientDeltaSignatures;
	}

	public LoadObjectsRequest getRequest() {
		return request;
	}

	public void putDomainModelHolder(DomainModelHolder domainModelHolder) {
		DomainTranche tranche = new DomainTranche();
		tranche.setDomainModelHolder(domainModelHolder);
		DomainModelDeltaTransport transport = new DomainModelDeltaTransport();
		transport.setDelta(tranche);
		deltaTransports.add(transport);
	}

	public void setAppInstruction(String appInstruction) {
		this.appInstruction = appInstruction;
	}

	public void setDeltaTransports(
			List<DomainModelDeltaTransport> deltaTransports) {
		this.deltaTransports = deltaTransports;
	}

	public void setPreserveClientDeltaSignatures(
			List<String> preserveClientDeltaSignatures) {
		this.preserveClientDeltaSignatures = preserveClientDeltaSignatures;
	}

	public void setRequest(LoadObjectsRequest request) {
		this.request = request;
	}

	public List<DomainModelDeltaTransport> getLoadSequenceTransports() {
		return this.loadSequenceTransports;
	}

	public void setLoadSequenceTransports(
			List<DomainModelDeltaTransport> loadSequenceTransports) {
		this.loadSequenceTransports = loadSequenceTransports;
	}

	
}
