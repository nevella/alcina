package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.List;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PropertyMapper {
	private PropertyAccessor leftAccessor;

	private PropertyAccessor rightAccessor;

	private List<PropertyMapping> mappings = new ArrayList<PropertyMapping>();

	public static class NoSuchVariantPropertyException
			extends RuntimeException {
		public NoSuchVariantPropertyException(String propertyName) {
			super(propertyName);
		}
	}

	public static class PropertyMapping {
		private String leftName;

		private String rightName;

		private Converter leftToRightConverter;

		private Converter rightToLeftConverter;

		private CollectionFilter applyToLeftFilter;

		private CollectionFilter applyToRightFilter;

		private boolean required;

		PropertyMapper mapper = null;

		@Override
		public String toString() {
			return CommonUtils.formatJ("propertyMapping: %s ->%s (custom: %s)",
					leftName, rightName, leftToRightConverter != null
							|| rightToLeftConverter != null);
		}

		public PropertyMapping() {
		}

		public PropertyMapping(String both) {
			this.rightName = both;
			this.leftName = both;
		}

		public PropertyMapping(String left, String right) {
			this.leftName = left;
			this.rightName = right;
		}

		void map(Object left, Object right) {
			if (applyToRightFilter != null && !applyToRightFilter.allow(left)) {
				return;
			}
			try {
				if (!required && !mapper.leftAccessor.hasPropertyKey(left,
						leftName)) {
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

		public PropertyMapping leftToRightConverter(Converter leftConverter) {
			this.leftToRightConverter = leftConverter;
			return this;
		}

		public PropertyMapping rightToLeftConverter(Converter rightConverter) {
			this.rightToLeftConverter = rightConverter;
			return this;
		}

		public PropertyMapping applyToLeftFilter(CollectionFilter leftFilter) {
			this.applyToLeftFilter = leftFilter;
			return this;
		}

		public PropertyMapping
				applyToRightFilter(CollectionFilter rightFilter) {
			this.applyToRightFilter = rightFilter;
			return this;
		}

		public PropertyMapping reverseMapping(PropertyMapper newMapper) {
			PropertyMapping mapping = new PropertyMapping();
			mapping.leftName = rightName;
			mapping.rightName = leftName;
			mapping.leftToRightConverter = rightToLeftConverter;
			mapping.rightToLeftConverter = leftToRightConverter;
			mapping.applyToLeftFilter = applyToRightFilter;
			mapping.applyToRightFilter = applyToLeftFilter;
			mapping.mapper = newMapper;
			return mapping;
		}

		public PropertyMapping bidiConverter(BidiConverter bidiConverter) {
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
	}

	public PropertyMapper reverseMapper() {
		PropertyMapper mapper = new PropertyMapper();
		mapper.leftAccessor = rightAccessor;
		mapper.rightAccessor = leftAccessor;
		for (PropertyMapping mapping : mappings) {
			mapper.mappings.add(mapping.reverseMapping(mapper));
		}
		return mapper;
	}

	public PropertyMapper() {
	}

	public PropertyMapper leftAccessor(PropertyAccessor accessor) {
		leftAccessor = accessor;
		return this;
	}

	public PropertyMapper rightAccessor(PropertyAccessor accessor) {
		rightAccessor = accessor;
		return this;
	}

	public void map(Object left, Object right) {
		map(left, right, null);
	}

	public void map(Object left, Object right, String leftKey) {
		for (PropertyMapping mapping : mappings) {
			if (leftKey == null || leftKey.equals(mapping.leftName)) {
				mapping.map(left, right);
			}
		}
	}

	public PropertyMapping define(String both) {
		return define(both, both);
	}

	public PropertyMapping define(String left, String right) {
		PropertyMapping mapping = new PropertyMapping(left, right);
		return addMapping(mapping);
	}

	public List<PropertyMapping> getMappings() {
		return this.mappings;
	}

	public PropertyMapping addMapping(PropertyMapping mapping) {
		mapping.mapper = this;
		mappings.add(mapping);
		return mapping;
	}
}
