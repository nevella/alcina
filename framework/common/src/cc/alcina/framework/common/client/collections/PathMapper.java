package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.util.Ax;

public class PathMapper {
	private PathAccessor leftAccessor;

	private PathAccessor rightAccessor;

	private List<PathMapping> mappings = new ArrayList<PathMapping>();

	public PathMapper() {
	}

	public PathMapping addMapping(PathMapping mapping) {
		mapping.mapper = this;
		mappings.add(mapping);
		return mapping;
	}

	public PathMapping define(String both) {
		return define(both, both);
	}

	public PathMapping define(String left, String right) {
		PathMapping mapping = new PathMapping(left, right);
		return addMapping(mapping);
	}

	public List<PathMapping> getMappings() {
		return this.mappings;
	}

	public PathMapper leftAccessor(PathAccessor accessor) {
		leftAccessor = accessor;
		return this;
	}

	public void map(Object left, Object right) {
		map(left, right, null);
	}

	public void map(Object left, Object right, String leftKey) {
		for (PathMapping mapping : mappings) {
			if (leftKey == null || leftKey.equals(mapping.leftName)) {
				mapping.map(left, right);
			}
		}
	}

	public PathMapper reverseMapper() {
		PathMapper mapper = new PathMapper();
		mapper.leftAccessor = rightAccessor;
		mapper.rightAccessor = leftAccessor;
		for (PathMapping mapping : mappings) {
			mapper.mappings.add(mapping.reverseMapping(mapper));
		}
		return mapper;
	}

	public PathMapper rightAccessor(PathAccessor accessor) {
		rightAccessor = accessor;
		return this;
	}

	public static class NoSuchVariantPropertyException
			extends RuntimeException {
		public NoSuchVariantPropertyException(String propertyName) {
			super(propertyName);
		}
	}

	public static class PathMapping {
		private String leftName;

		private String rightName;

		private Converter leftToRightConverter;

		private Converter rightToLeftConverter;

		private Predicate applyToLeftFilter;

		private Predicate applyToRightFilter;

		private boolean required;

		PathMapper mapper = null;

		public PathMapping() {
		}

		public PathMapping(String both) {
			this.rightName = both;
			this.leftName = both;
		}

		public PathMapping(String left, String right) {
			this.leftName = left;
			this.rightName = right;
		}

		public PathMapping applyToLeftFilter(Predicate leftFilter) {
			this.applyToLeftFilter = leftFilter;
			return this;
		}

		public PathMapping applyToRightFilter(Predicate rightFilter) {
			this.applyToRightFilter = rightFilter;
			return this;
		}

		public PathMapping bidiConverter(BidiConverter bidiConverter) {
			this.leftToRightConverter = bidiConverter.leftToRightConverter();
			this.rightToLeftConverter = bidiConverter.rightToLeftConverter();
			return this;
		}

		public String getLeftName() {
			return this.leftName;
		}

		public String getRightName() {
			return this.rightName;
		}

		public PathMapping leftToRightConverter(Converter leftConverter) {
			this.leftToRightConverter = leftConverter;
			return this;
		}

		void map(Object left, Object right) {
			if (applyToRightFilter != null && !applyToRightFilter.test(left)) {
				return;
			}
			try {
				// If mapped field is not required, and either left object is
				// null or
				// the property key is not available on the left object
				if (!required && (left == null || !mapper.leftAccessor
						.hasPropertyKey(left, leftName))) {
					return;
				}
				Object value = mapper.leftAccessor.getPropertyValue(left,
						leftName);
				if (leftToRightConverter != null) {
					value = leftToRightConverter.convert(value);
				}
				mapper.rightAccessor.setPropertyValue(right, rightName, value);
			} catch (NoSuchVariantPropertyException e) {
				if (required) {
					throw e;
				}
			}
		}

		public void required() {
			required = true;
		}

		public PathMapping reverseMapping(PathMapper newMapper) {
			PathMapping mapping = new PathMapping();
			mapping.leftName = rightName;
			mapping.rightName = leftName;
			mapping.leftToRightConverter = rightToLeftConverter;
			mapping.rightToLeftConverter = leftToRightConverter;
			mapping.applyToLeftFilter = applyToRightFilter;
			mapping.applyToRightFilter = applyToLeftFilter;
			mapping.mapper = newMapper;
			return mapping;
		}

		public PathMapping rightToLeftConverter(Converter rightConverter) {
			this.rightToLeftConverter = rightConverter;
			return this;
		}

		@Override
		public String toString() {
			return Ax.format("propertyMapping: %s ->%s (custom: %s)", leftName,
					rightName, leftToRightConverter != null
							|| rightToLeftConverter != null);
		}
	}
}
