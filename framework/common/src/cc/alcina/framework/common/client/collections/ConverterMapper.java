package cc.alcina.framework.common.client.collections;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PathMapper.PathMapping;
import cc.alcina.framework.common.client.reflection.Reflections;

public class ConverterMapper<A, B> implements Converter<A, B> {
	protected Class<A> leftClass;

	protected Class<B> rightClass;

	protected PathMapper mapper;

	protected AuxiliaryMapper<A, B> leftAuxiliary;

	protected AuxiliaryMapper<B, A> rightAuxiliary;

	protected Supplier<A> leftSupplier;

	protected Supplier<B> rightSupplier;

	public ConverterMapper() {
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

	public BidiConverterAdapter bidiAdapter() {
		return new BidiConverterAdapter();
	}

	@Override
	public B convert(A a) {
		try {
			B b = rightSupplier == null ? Reflections.newInstance(rightClass)
					: rightSupplier.get();
			apply(a, b);
			return b;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<String> getLeftPropertyNames() {
		return mapper.getMappings().stream().map(m -> m.getLeftName())
				.collect(Collectors.toList());
	}

	public List<String> getRightPropertyNames() {
		return mapper.getMappings().stream().map(m -> m.getRightName())
				.collect(Collectors.toList());
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

	protected PathMapping define(PathMapping mapping) {
		return mapper.addMapping(mapping);
	}

	protected PathMapping define(String both) {
		return define(both, both);
	}

	protected PathMapping define(String leftPropertyName,
			String rightPropertyName) {
		return mapper.define(leftPropertyName, rightPropertyName);
	}

	protected PathMapping defineLeftCamel(String propertyName) {
		return mapper.define(propertyName,
				propertyName.substring(0, 1).toLowerCase()
						+ propertyName.substring(1).replace("ID", "Id"));
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
}
