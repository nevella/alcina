package cc.alcina.framework.classmeta;

import java.net.ConnectException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;

public class ClassMetaInvoker {
	public ClassMetaResponse invoke(ClassMetaRequest metaRequest) {
		try {
			LooseContext.pushWithTrue(
					JacksonJsonObjectSerializer.CONTEXT_WITHOUT_MAPPER_POOL);
			String url = ResourceUtilities.get(ClassMetaInvoker.class,
					"remoteScannerUrl");
			String json = ResourceUtilities.readUrlAsStringWithPost(url,
					JacksonUtils.serialize(metaRequest), new StringMap());
			return JacksonUtils.deserialize(json, ClassMetaResponse.class);
		} catch (Exception e) {
			if (CommonUtils.extractCauseOfClass(e,
					ConnectException.class) != null) {
				Ax.err("ClassMetaServer not reachable");
				return null;
			}
			throw new WrappedRuntimeException(e);
		} finally {
			LooseContext.pop();
		}
	}
}
