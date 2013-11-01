package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DomainModelDeltaLookup {
	public Map<String, DomainModelDeltaSignature> nonVersionedSignatures = new LinkedHashMap<String, DomainModelDeltaSignature>();

	public Map<DomainModelDeltaSignature, DomainModelDeltaMetadata> metadataCache = new LinkedHashMap<DomainModelDeltaSignature, DomainModelDeltaMetadata>();

	public List<String> versionedSignatures = new ArrayList<String>();

	public List<String> existingKeys;

	public void addSignature(String signatureString) {
		DomainModelDeltaSignature signature = DomainModelDeltaSignature
				.parseSignature(signatureString);
		nonVersionedSignatures
				.put(signature.nonVersionedSignature(), signature);
		versionedSignatures.add(signature.toString());
	}
}