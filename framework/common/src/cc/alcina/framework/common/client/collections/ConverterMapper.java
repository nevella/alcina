package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.totsp.gwittir.client.beans.Converter;

public class ConverterMapper<A, B> implements Converter<A, B> {
	protected Class<A> leftClass;

	protected Class<B> rightClass;

	protected PropertyMapper mapper;

	public ConverterMapper() {
	}

	public ConverterMapper<B, A> invert() {
		ConverterMapper result = new ConverterMapper();
		result.leftClass = rightClass;
		result.rightClass = leftClass;
		result.mapper = mapper.reverseMapper();
		return result;
	}

	@Override
	public B convert(A a) {
		try {
			B b = rightClass.newInstance();
			apply(a, b);
			return b;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		
	}

	
	public void apply(A a, B b) {
		try {
			mapper.map(a, b);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
