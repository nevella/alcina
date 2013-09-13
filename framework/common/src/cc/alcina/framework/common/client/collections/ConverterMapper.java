package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyMapper.PropertyMapping;

import com.totsp.gwittir.client.beans.Converter;

public class ConverterMapper<A, B> implements Converter<A, B> {
	protected Class<A> leftClass;

	protected Class<B> rightClass;

	protected PropertyMapper mapper;

	protected AuxiliaryMapper<A, B> leftAuxiliary;

	protected AuxiliaryMapper<B, A> rightAuxiliary;

	public ConverterMapper() {
	}

	public ConverterMapper<B, A> invert() {
		ConverterMapper result = new ConverterMapper();
		result.leftClass = rightClass;
		result.rightClass = leftClass;
		result.leftAuxiliary = rightAuxiliary;
		result.rightAuxiliary = leftAuxiliary;
		result.mapper = mapper.reverseMapper();
		return result;
	}

	@Override
	public B convert(A a) {
		try {
			B b = CommonLocator.get().classLookup().newInstance(rightClass);
			apply(a, b);
			return b;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void apply(A a, B b) {
		try {
			mapper.map(a, b);
			if (leftAuxiliary != null) {
				leftAuxiliary.map(a, b);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected PropertyMapping define(String leftPropertyName,
			String rightPropertyName) {
		return mapper.define(leftPropertyName, rightPropertyName);
	}

	protected PropertyMapping define(String both) {
		return define(both, both);
	}
}
