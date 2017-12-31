package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DomainModelDeltaLookup {
	public Map<String, DomainModelDeltaSignature> nonVersionedSignatures = new LinkedHashMap<String, DomainModelDeltaSignature>();

	public Map<DomainModelDeltaSignature, DomainModelDeltaMetadata> metadataCache = new LinkedHashMap<DomainModelDeltaSignature, DomainModelDeltaMetadata>();

	public Map<String, DomainModelDelta> deltaCache = new LinkedHashMap<String, DomainModelDelta>();

	public Map<DomainModelDeltaSignature, String> contentCache = new LinkedHashMap<DomainModelDeltaSignature, String>();

	public List<String> versionedSignatures = new ArrayList<String>();

	public List<String> existingKeys;

	public void addSignature(String signatureString) {
		DomainModelDeltaSignature signature = DomainModelDeltaSignature
				.parseSignature(signatureString);
		nonVersionedSignatures.put(signature.nonVersionedSignature(),
				signature);
		versionedSignatures.add(signature.toString());
	}

	public boolean hasNoSerializedContent(String nonVersionedKey) {
		DomainModelDeltaSignature sig = nonVersionedSignatures
				.get(nonVersionedKey);
		return sig != null && contentCache.containsKey(sig)
				&& contentCache.get(sig) == null;
	}

	public void invalidate(DomainModelDeltaSignature sig) {
		String nonVersionedSignature = sig.nonVersionedSignature();
		if (nonVersionedSignatures.containsKey(nonVersionedSignature)) {
			DomainModelDeltaSignature sig2 = nonVersionedSignatures
					.get(nonVersionedSignature);
			contentCache.remove(sig2);
			metadataCache.remove(sig2);
			deltaCache.remove(nonVersionedSignature);
			nonVersionedSignatures.remove(nonVersionedSignature);
		}
	}
}