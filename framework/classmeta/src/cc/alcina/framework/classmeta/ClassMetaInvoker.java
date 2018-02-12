package cc.alcina.framework.classmeta;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClassMetaInvoker {
	public ClassMetaResponse invoke(ClassMetaRequest metaRequest) {
		try {
			String url = ResourceUtilities.get(ClassMetaInvoker.class,
					"remoteScannerUrl");
			byte[] bytes = ResourceUtilities.readUrlAsBytesWithPost(url,
					KryoUtils.serializeToBase64(metaRequest, true), new StringMap());
			return KryoUtils.deserializeFromByteArray(bytes,
					ClassMetaResponse.class,true);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
