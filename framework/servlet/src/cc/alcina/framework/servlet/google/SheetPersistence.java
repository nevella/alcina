package cc.alcina.framework.servlet.google;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Objects;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

public class SheetPersistence {
	private Object persistent;

	private transient SheetAccessor sheetAccesor;

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
	}

	public void load(boolean useLocalCached) {
		try {
			ensureFields();
			if (useLocalCached) {
				File file = getPersistentFile();
				if (file.exists()) {
					try {
						list = KryoUtils.deserializeFromFile(file,
								ArrayList.class);
						listField.set(persistent, list);
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			logger.info("Loading {}", persistent.getClass().getSimpleName());
			list.clear();
			for (SheetWrapper.Row row : sheetWrapper()) {
				boolean hadValue = false;
				Object v = Reflections.newInstance(valueType);
				for (Field f : valueTypeFields) {
					String value = row.getValue(translateFieldName(f));
					if (value != null) {
						hadValue = true;
						if (f.getType() == int.class) {
							f.set(v, Integer.parseInt(value.toString()));
						} else if (f.getType() == String.class) {
							f.set(v, value);
						} else {
							throw new IllegalArgumentException();
						}
					}
				}
				if (hadValue) {
					if (v instanceof SheetRow) {
						((SheetRow) v).setRowIdx(row.idx);
						((SheetRow) v).setSheetName(sheetAccess.getSheetName());
					}
					list.add(v);
				} else {
					break;
				}
			}
			listField.set(persistent, list);
			KryoUtils.serializeToFile(list, getPersistentFile());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void save(boolean dryRun) {
		try {
			KryoUtils.serializeToFile(list, getPersistentFile());
			logger.info("Saving {} - dry run: {}",
					persistent.getClass().getSimpleName(), dryRun);
			ensureFields();
			SheetWrapper sheetWrapper = sheetWrapper();
			boolean hadUpdate = false;
			if (list.size() > 0 && list.iterator().next() instanceof Comparable
					&& isSortBeforeSave()) {
				list.sort(Comparator.naturalOrder());
			}
			int rowIdx = 0;
			for (; rowIdx < list.size(); rowIdx++) {
				SheetWrapper.Row row = sheetWrapper.getRow(rowIdx);
				boolean hadValue = false;
				Object v = list.get(rowIdx);
				for (Field f : valueTypeFields) {
					String value = null;
					Object fValue = f.get(v);
					if (fValue != null) {
						if (f.getType() == int.class) {
							value = String.valueOf(fValue);
						} else if (f.getType() == String.class) {
							value = fValue.toString();
						} else {
							throw new IllegalArgumentException();
						}
					}
					String existing = row.getValue(translateFieldName(f));
					if (Objects.equal(value, existing)) {
						continue;
					}
					hadUpdate = true;
					Ax.out("\tUpdate %s::%s :: %s => %s",
							sheetAccess.getSheetName(), rowIdx + 1, existing,
							value);
					if (dryRun) {
					} else {
						sheetWrapper.addUpdate(translateFieldName(f), value,
								row);
					}
				}
			}
			for (; rowIdx < sheetWrapper.rowSize(); rowIdx++) {
				SheetWrapper.Row row = sheetWrapper.getRow(rowIdx);
				boolean hadValue = false;
				Object value = null;
				for (Field f : valueTypeFields) {
					String existing = row.getValue(translateFieldName(f));
					if (Objects.equal(value, existing)) {
						continue;
					}
					hadUpdate = true;
					Ax.out("\tUpdate %s::%s :: %s => %s",
							sheetAccess.getSheetName(), rowIdx + 1, existing,
							value);
					if (dryRun) {
					} else {
						sheetWrapper.addUpdate(translateFieldName(f), value,
								row);
					}
				}
			}
			if (!dryRun && hadUpdate) {
				sheetAccesor.update(sheetWrapper.batchUpdateRequest);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void ensureFields()
			throws IllegalArgumentException, IllegalAccessException {
		if (listField == null) {
			listField = Arrays.stream(persistent.getClass().getDeclaredFields())
					.filter(f -> f.getType() == List.class
							&& !Modifier.isTransient(f.getModifiers()))
					.findFirst().get();
			listField.setAccessible(true);
			Type pt = GraphProjection.getGenericType(listField);
			Type genericType = ((ParameterizedType) pt)
					.getActualTypeArguments()[0];
			valueType = (Class) genericType;
			Arrays.stream(valueType.getConstructors())
					.forEach(c -> c.setAccessible(true));
			valueTypeFields = Arrays.stream(valueType.getDeclaredFields())
					.filter(f -> !Modifier.isTransient(f.getModifiers()))
					.peek(f -> f.setAccessible(true))
					.collect(Collectors.toList());
			list = (List) listField.get(persistent);
		}
	}

	private File getPersistentFile() {
		DataFolderProvider.get().getSubFolder("sheetPersistence").mkdirs();
		return DataFolderProvider.get()
				.getChildFile(Ax.format("sheetPersistence/%s.dat",
						persistent.getClass().getSimpleName()));
	}

	protected boolean isSortBeforeSave() {
		return true;
	}

	protected String translateFieldName(Field f) {
		return f.getName().replace("_", " ");
	}

	SheetWrapper sheetWrapper() {
		this.sheetAccesor = new SheetAccessor().withSheetAccess(sheetAccess);
		return new SheetWrapper(
				sheetAccesor.getSheet(sheetAccesor.sheetAccess.getSheetName()));
	}
}
