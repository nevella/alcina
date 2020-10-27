package cc.alcina.framework.servlet.google;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.api.client.util.Objects;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;

public class GoogleSheetWrapper implements Iterable<GoogleSheetWrapper.Row>,
		Iterator<GoogleSheetWrapper.Row> {
	private List<RowData> rowData;

	int rowIdx = 1;

	Sheet sheet;

	private List<GridData> data;

	private CachingMap<String, Integer> keyLookup = new CachingMap<>(key -> {
		List<CellData> headers = rowData.get(0).getValues();
		for (int idx = 0; idx < headers.size(); idx++) {
			CellData c = headers.get(idx);
			ExtendedValue effectiveValue = c.getEffectiveValue();
			if (effectiveValue == null) {
				continue;
			}
			if (effectiveValue.getStringValue().equals(key)) {
				return idx;
			}
		}
		throw new IndexOutOfBoundsException(key);
	});

	public BatchUpdateValuesRequest batchUpdateRequest;

	public GoogleSheetWrapper(Sheet sheet) {
		this.sheet = sheet;
		data = sheet.getData();
		rowData = data.get(0).getRowData();
		batchUpdateRequest = new BatchUpdateValuesRequest();
		batchUpdateRequest.setData(new ArrayList<>());
		batchUpdateRequest.setValueInputOption("USER_ENTERED");
	}

	public void clearAfter(int rowCounter) {
		rowIdx = rowCounter;
		while (hasNext()) {
			Row row = next();
			row.clear();
		}
	}

	@Override
	public boolean hasNext() {
		return rowData.size() > rowIdx;
	}

	@Override
	public Iterator<GoogleSheetWrapper.Row> iterator() {
		return this;
	}

	@Override
	public Row next() {
		return new Row(rowIdx++);
	}

	public void setDateValue(int rowCounter, String key, String formattedDate) {
		Row row = new Row(rowCounter);
		String existing = Ax.blankToEmpty(row.getDateValue(key));
		String normalisedFormatted = formattedDate.replaceAll("(^|/)0", "$1");
		if (Objects.equal(existing, normalisedFormatted)
				|| Objects.equal(existing, formattedDate)) {
		} else {
			addUpdate(key, formattedDate, row);
		}
	}

	public void setNumericValue(int rowCounter, String key, Double value) {
		Row row = new Row(rowCounter);
		Double existing = row.getNumberValue(key);
		if (Objects.equal(existing, value)) {
		} else {
			addUpdate(key, value, row);
		}
	}

	public void setStringValue(int rowCounter, String key, Object value) {
		Row row = new Row(rowCounter);
		String existing = row.getValue(key);
		if (Objects.equal(existing, value)
				|| (existing == null && value.toString().equals(""))) {
		} else {
			addUpdate(key, value, row);
		}
	}

	private int indexOf(String key) {
		return keyLookup.get(key);
	}

	protected void addUpdate(String key, Object value, Row row) {
		ValueRange valueRange = row.getUpdateRange(key);
		List<Object> cellValues = new ArrayList<>();
		cellValues.add(value);
		List<List<Object>> updateValues = new ArrayList<>();
		updateValues.add(cellValues);
		valueRange.setValues(updateValues);
		batchUpdateRequest.getData().add(valueRange);
	}

	public class Row {
		public int idx;

		public Row(int idx) {
			this.idx = idx;
		}

		public void clear() {
			List<CellData> values = idx >= rowData.size() ? null
					: rowData.get(idx).getValues();
			if (values == null) {
				return;
			}
			for (String key : keyLookup.getMap().keySet()) {
				addUpdate(key, "", this);
			}
		}

		public String getDateValue(String key) {
			return withCellData(key, cd -> cd.getFormattedValue());
		}

		public Double getNumberValue(String key) {
			return withCellData(key,
					cd -> cd.getEffectiveValue().getNumberValue());
		}

		public ValueRange getUpdateRange(String key) {
			ValueRange valueRange = new ValueRange();
			int colIndexAscii = 'A' + indexOf(key);
			char colIndex = (char) colIndexAscii;
			valueRange.setRange(Ax.format("%s!%s%s",
					sheet.getProperties().getTitle(), colIndex, idx + 1));
			return valueRange;
		}

		public String getValue(String key) {
			return withCellData(key, cd -> {
				String value = cd.getEffectiveValue().getStringValue();
				if (value == null) {
					Double number = cd.getEffectiveValue().getNumberValue();
					if (number != null) {
						if (Math.floor(number) == number.doubleValue()) {
							return String.valueOf(number.intValue());
						} else {
							throw new UnsupportedOperationException(
									"Not expecting non-integral number");
						}
					}
				}
				return value;
			});
		}

		private CellData getCellData(String key) {
			List<CellData> values = idx >= rowData.size() ? null
					: rowData.get(idx).getValues();
			if (values == null) {
				return null;
			}
			int colIdx = indexOf(key);
			CellData cellData = colIdx >= values.size() ? null
					: values.get(colIdx);
			if (cellData == null || cellData.getEffectiveValue() == null) {
				return null;
			}
			return cellData;
		}

		private <T> T withCellData(String key, Function<CellData, T> mapper) {
			return Optional.<CellData> ofNullable(getCellData(key)).map(mapper)
					.orElse(null);
		}
	}
}