package cc.alcina.framework.servlet.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.projection.GraphProjections;

public abstract class GwtRpcRequestHandler<I, O> {
	public static <I, O> O evaluate(I input) {
		try {
			return (O) Registry
					.impl(GwtRpcRequestHandler.class, input.getClass())
					.handle(input);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());

	public abstract O handle(I in) throws Exception;

	protected <T> T defaultProjection(T t) {
		return GraphProjections.defaultProjections().project(t);
	}
}
