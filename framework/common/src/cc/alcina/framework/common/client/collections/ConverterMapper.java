package cc.alcina.framework.common.client.collections;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyMapper.PropertyMapping;

public class ConverterMapper<A, B> implements Converter<A, B> {
	protected Class<A> leftClass;

	protected Class<B> rightClass;

	protected PropertyMapper mapper;

	protected AuxiliaryMapper<A, B> leftAuxiliary;

	protected AuxiliaryMapper<B, A> rightAuxiliary;

	protected Supplier<A> leftSupplier;

	protected Supplier<B> rightSupplier;

	public ConverterMapper() {
	}

	public BidiConverterAdapter bidiAdapter() {
		return new BidiConverterAdapter();
	}

	public class BidiConverterAdapter extends BidiConverter<A, B> {
		@Override
		public B leftToRight(A a) {
			return convert(a);
		}

		@Override
		public A rightToLeft(B b) {
			return invert().convert(b);
		}
	}

	public ConverterMapper<B, A> invert() {
		ConverterMapper result = new ConverterMapper();
		result.leftClass = rightClass;
		result.rightClass = leftClass;
		result.leftAuxiliary = rightAuxiliary;
		result.rightAuxiliary = leftAuxiliary;
		result.leftSupplier = rightSupplier;
		result.rightSupplier = leftSupplier;
		result.mapper = mapper.reverseMapper();
		return result;
	}

	public List<String> getLeftPropertyNames() {
		return mapper.getMappings().stream().map(m -> m.getLeftName())
				.collect(Collectors.toList());
	}
	public List<String> getRightPropertyNames() {
		return mapper.getMappings().stream().map(m -> m.getRightName())
				.collect(Collectors.toList());
	}

	@Override
	public B convert(A a) {
		try {
			B b = rightSupplier == null
					? Reflections.classLookup().newInstance(rightClass)
					: rightSupplier.get();
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
