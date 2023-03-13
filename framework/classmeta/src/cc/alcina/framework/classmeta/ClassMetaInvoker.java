package cc.alcina.framework.classmeta;

import java.net.ConnectException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.MethodContext;

// FIXME - ru - delete project
public class ClassMetaInvoker {
	public ClassMetaResponse invoke(ClassMetaRequest metaRequest) {
		return MethodContext.instance()
				.withContextTrue(
						JacksonJsonObjectSerializer.CONTEXT_WITHOUT_MAPPER_POOL)
				.withContextClassloader(getClass().getClassLoader())
				.call(() -> {
					try {
						String url = Configuration.get("remoteScannerUrl");
						String json = new SimpleHttp(url)
								.withPostBody(
										JacksonUtils.serialize(metaRequest))
								.asString();
						return JacksonUtils.deserialize(json,
								ClassMetaResponse.class);
					} catch (Exception e) {
						if (CommonUtils.extractCauseOfClass(e,
								ConnectException.class) != null) {
							Ax.err("ClassMetaServer not reachable");
							return null;
						}
						throw new WrappedRuntimeException(e);
					}
				});
	}
}
