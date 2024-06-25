package cc.alcina.framework.servlet.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.entity.projection.GraphProjections;

public abstract class GwtRpcRequestHandler<I, O> {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	public abstract O handle(I in) throws Exception;

	protected <T> T defaultProjection(T t) {
		return GraphProjections.defaultProjections().project(t);
	}
}
