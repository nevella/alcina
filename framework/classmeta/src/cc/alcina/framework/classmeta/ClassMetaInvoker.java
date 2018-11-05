package cc.alcina.framework.classmeta;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClassMetaInvoker {
	public ClassMetaResponse invoke(ClassMetaRequest metaRequest) {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER);
			String url = ResourceUtilities.get(ClassMetaInvoker.class,
					"remoteScannerUrl");
			byte[] bytes = ResourceUtilities.readUrlAsBytesWithPost(url,
					KryoUtils.serializeToBase64(metaRequest), new StringMap());
			return KryoUtils.deserializeFromByteArray(bytes,
					ClassMetaResponse.class);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			LooseContext.pop();
		}
	}
}
