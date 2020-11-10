package cc.alcina.framework.servlet.google;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

public class SheetPersistence {
	private Object persistent;

	private transient SheetAccessor sheetAccesor;

	private transient SheetWrapper sheetWrapper;

	private transient Class valueType;

	private transient List list;

	private transient List<Field> valueTypeFields;

	private SheetAccess sheetAccess;

	private transient Field listField;

	private transient Logger logger = LoggerFactory.getLogger(getClass());

	public SheetPersistence() {
	}

	public SheetPersistence(Object persistent, SheetAccess sheetAccess) {
		this.persistent = persistent;
		this.sheetAccess = sheetAccess;
		Preconditions.checkArgument(
				persistent.getClass().getDeclaredFields().length == 1);
	}

	public void load() {
		try {
			logger.info("Loading {}", persistent.getClass().getSimpleName());
			ensureFields();
			list.clear();
			int rowIdx = 0;
			for (SheetWrapper.Row row : sheetWrapper()) {
				boolean hadValue = false;
				Object v = Reflections.newInstance(valueType);
				for (Field f : valueTypeFields) {
					String value = row.getValue(f.getName().replace("_", " "));
					if (value != null) {
						hadValue = true;
						f.set(v, value);
					}
				}
				if (hadValue) {
					list.add(v);
				} else {
					break;
				}
			}
			listField.set(persistent, list);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void save() {
	}

	private void ensureFields()
			throws IllegalArgumentException, IllegalAccessException {
		if (listField == null) {
			listField = Arrays.stream(persistent.getClass().getDeclaredFields())
					.filter(f -> f.getType() == List.class).findFirst().get();
			listField.setAccessible(true);
			Type pt = GraphProjection.getGenericType(listField);
			Type genericType = ((ParameterizedType) pt)
					.getActualTypeArguments()[0];
			valueType = (Class) genericType;
			Arrays.stream(valueType.getConstructors())
					.forEach(c -> c.setAccessible(true));
			valueTypeFields = Arrays.stream(valueType.getDeclaredFields())
					.peek(f -> f.setAccessible(true))
					.collect(Collectors.toList());
			list = (List) listField.get(persistent);
		}
	}

	SheetWrapper sheetWrapper() {
		this.sheetAccesor = new SheetAccessor().withSheetAccess(sheetAccess);
		this.sheetWrapper = new SheetWrapper(
				sheetAccesor.getSheet(sheetAccesor.sheetAccess.getSheetName()));
		return this.sheetWrapper;
	}
}
