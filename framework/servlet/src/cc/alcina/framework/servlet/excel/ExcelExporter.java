/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.excel;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.ConverterFilter;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.publication.excel.ExcelFormatAnnotation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.XmlUtils;

/**
 * 
 * @author Nick Reddel
 */
public class ExcelExporter {
	private static final String SS_NS = "urn:schemas-microsoft-com:office:spreadsheet";

	private static final String DOC_TEMPLATE_XML = "docTemplate.xml";

	private Element sheetTemplate = null;

	private final class ToPdMultiConverterFilter implements
			ConverterFilter<PropertyDescriptor, ExcelExporter.PdMultiplexer> {
		@Override
		public PdMultiplexer convert(PropertyDescriptor original) {
			return new PdMultiplexer(original);
		}

		@Override
		public boolean allowPreConvert(PropertyDescriptor t) {
			return !ignorePd(t);
		}

		@Override
		public boolean allowPostConvert(PdMultiplexer c) {
			return true;
		}
	}

	public static class ExcelEmptyBean implements Serializable {
		private String name = "No data";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public void addCollectionToBook(Collection coll, Document book,
			String sheetName) throws Exception {
		if (!coll.iterator().hasNext()) {
			coll = new ArrayList();
			coll.add(new ExcelEmptyBean());
		}
		Object o = coll.iterator().next();
		Class clazz = o.getClass();
		BeanInfo beanInfo = ResourceUtilities.getBeanInfo(clazz);
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		List<PdMultiplexer> pdMultis = CollectionFilters.convertAndFilter(
				Arrays.asList(pds), new ToPdMultiConverterFilter());
		Collections.sort(pdMultis);
		addCollectionToBook(coll, book, sheetName, pdMultis);
	}

	private List<List> cellList = new ArrayList<>();

	public List<List> getCellList() {
		return this.cellList;
	}

	static class PdMultiplexer implements Comparable<PdMultiplexer> {
		private ExcelFormatAnnotation xfa;

		private VisualiserInfo via;

		PropertyDescriptor pd;

		public PdMultiplexer(PropertyDescriptor pd) {
			this.pd = pd;
			Method readMethod = pd.getReadMethod();
			this.xfa = readMethod.getAnnotation(ExcelFormatAnnotation.class);
			this.via = readMethod.getAnnotation(VisualiserInfo.class);
		}

		public int order() {
			return xfa != null ? xfa.order() : via != null ? via.displayInfo()
					.orderingHint() : ExcelFormatAnnotation.DEFAULT_ORDER_POS;
		}

		public String name() {
			return xfa != null && !xfa.displayName().isEmpty() ? xfa
					.displayName() : via != null ? via.displayInfo().name()
					: pd.getName();
		}

		@Override
		public int compareTo(PdMultiplexer o) {
			return order() - o.order();
		}

		public String styleId() {
			return xfa == null ? "" : xfa.styleId();
		}
	}

	public void addCollectionToBook(Collection coll, Document book,
			String sheetName, PropertyDescriptor[] pds) throws Exception {
		List<PdMultiplexer> pdMultis = CollectionFilters.convertAndFilter(
				Arrays.asList(pds), new ToPdMultiConverterFilter());
		addCollectionToBook(coll, book, sheetName, pds);
	}

	private void addCollectionToBook(Collection coll, Document book,
			String sheetName, List<PdMultiplexer> pds) throws Exception {
		if (!coll.iterator().hasNext()) {
			return;
		}
		Element sn = (Element) sheetTemplate.cloneNode(true);
		sn.setAttributeNS(SS_NS, "ss:Name", sheetName);
		book.getDocumentElement().appendChild(sn);
		Element table = (Element) sn.getElementsByTagName("Table").item(0);
		Object o = coll.iterator().next();
		Class clazz = o.getClass();
		BeanInfo beanInfo = ResourceUtilities.getBeanInfo(clazz);
		table.setAttributeNS(SS_NS, "ss:ExpandedColumnCount",
				String.valueOf(pds.size()));
		table.setAttributeNS(SS_NS, "ss:ExpandedRowCount",
				String.valueOf(coll.size() + 1));
		Element row;
		List dataRow;
		Element cell;
		Element data;
		Text txt;
		row = book.createElement("Row");
		dataRow = new ArrayList<>();
		row.setAttributeNS(SS_NS, "ss:StyleID", "sHeaderRow");
		for (PdMultiplexer pdm : pds) {
			Element col = book.createElement("Column");
			col.setAttributeNS(SS_NS, "ss:AutoFitWidth", "0");
			col.setAttributeNS(SS_NS, "ss:Width", "120");
			String styleId = null;
			if (pdm.pd.getPropertyType() == Date.class) {
				styleId = "sDate";
			}
			if (!pdm.styleId().isEmpty()) {
				styleId = pdm.styleId();
			}
			if (styleId != null) {
				col.setAttributeNS(SS_NS, "ss:StyleID", styleId);
			}
			table.appendChild(col);
			cell = book.createElement("Cell");
			data = book.createElement("Data");
			row.appendChild(cell);
			cell.appendChild(data);
			data.setAttributeNS(SS_NS, "ss:Type", "String");
			txt = book.createTextNode(colnameFromFieldname(pdm.name()));
			dataRow.add(txt.getTextContent());
			data.appendChild(txt);
		}
		table.appendChild(row);
		cellList.add(dataRow);
		// 1970-01-01T00:00:00.000
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		for (Iterator it = coll.iterator(); it.hasNext();) {
			o = it.next();
			row = book.createElement("Row");
			dataRow = new ArrayList<>();
			int colIndex = 1;
			for (PdMultiplexer pdm : pds) {
				cell = book.createElement("Cell");
				data = book.createElement("Data");
				cell.appendChild(data);
				String type = "String";
				Object value = pdm.pd.getReadMethod().invoke(o,
						CommonUtils.EMPTY_OBJECT_ARRAY);
				cell.setAttributeNS(SS_NS, "ss:Index",
						String.valueOf(colIndex++));
				if (value == null
						|| (value instanceof String && value.toString()
								.isEmpty())) {
					dataRow.add(null);
					continue;
				}
				if (pdm.pd.getPropertyType() == Date.class) {
					type = "DateTime";
					value = df.format(value);
				} else if (Number.class.isAssignableFrom(pdm.pd
						.getPropertyType())) {
					type = "Number";
				}
				data.setAttributeNS(SS_NS, "ss:Type", type);
				txt = book.createTextNode((value == null) ? "" : value
						.toString());
				data.appendChild(txt);
				dataRow.add(txt.getTextContent());
				row.appendChild(cell);
			}
			table.appendChild(row);
			cellList.add(dataRow);
		}
	}

	private boolean ignorePd(PropertyDescriptor pd) {
		if (pd.getReadMethod() == null) {
			return true;
		}
		ExcelFormatAnnotation ann = pd.getReadMethod().getAnnotation(
				ExcelFormatAnnotation.class);
		return (pd.getName().equals("class")
				|| pd.getName().equals("propertyChangeListeners")
				|| pd.getName().equals("localId") || pd.getName().equals(
				"versionNumber"))
				|| (ann != null && ann.omit());
	}

	public void add2dCollectionToBook(Collection coll, Document book,
			String sheetName) throws Exception {
		Iterator topItr = coll.iterator();
		if (!topItr.hasNext()) {
			return;
		}
		Element sn = (Element) sheetTemplate.cloneNode(true);
		sn.setAttributeNS(SS_NS, "ss:Name", sheetName);
		book.getDocumentElement().appendChild(sn);
		Element table = (Element) sn.getElementsByTagName("Table").item(0);
		Collection rowCollection = (Collection) topItr.next();
		table.setAttributeNS(SS_NS, "ss:ExpandedColumnCount",
				String.valueOf(rowCollection.size()));
		table.setAttributeNS(SS_NS, "ss:ExpandedRowCount",
				String.valueOf(coll.size()));
		Element row;
		Element cell;
		Element data;
		Text txt;
		row = book.createElement("Row");
		row.setAttributeNS(SS_NS, "ss:StyleID", "sHeaderRow");
		for (Iterator itr = rowCollection.iterator(); itr.hasNext();) {
			Object value = itr.next();
			Element col = book.createElement("Column");
			col.setAttributeNS(SS_NS, "ss:AutoFitWidth", "0");
			col.setAttributeNS(SS_NS, "ss:Width", "120");
			table.appendChild(col);
			cell = book.createElement("Cell");
			data = book.createElement("Data");
			row.appendChild(cell);
			cell.appendChild(data);
			data.setAttributeNS(SS_NS, "ss:Type", "String");
			txt = book.createTextNode(strVal(value));
			data.appendChild(txt);
		}
		table.appendChild(row);
		for (Iterator it = topItr; it.hasNext();) {
			rowCollection = (Collection) it.next();
			row = book.createElement("Row");
			int colIndex = 1;
			for (Iterator itr = rowCollection.iterator(); itr.hasNext();) {
				Object value = itr.next();
				cell = book.createElement("Cell");
				data = book.createElement("Data");
				cell.setAttributeNS(SS_NS, "ss:Index",
						String.valueOf(colIndex++));
				if (value == null) {
					continue;
				}
				row.appendChild(cell);
				cell.appendChild(data);
				String strVal = strVal(value);
				data.setAttributeNS(SS_NS, "ss:Type",
						isNumeric(strVal) ? "Number" : "String");
				Matcher m = percentPattern.matcher(strVal);
				if (m.matches()) {
					Double v2 = Double.parseDouble(m.group(1)) / 100;
					strVal = v2.toString();
					data.setAttributeNS(SS_NS, "ss:Type", "Number");
					cell.setAttributeNS(SS_NS, "ss:StyleID", "sPercent");
				}
				txt = book.createTextNode(strVal);
				data.appendChild(txt);
			}
			table.appendChild(row);
		}
	}

	private Pattern percentPattern = Pattern
			.compile("-?(([0-9]+\\.?[0-9]*)|([0-9]*\\.?[0-9]+))%");

	private Pattern numericPattern = Pattern
			.compile("-?([0-9]+\\.?[0-9]*)|([0-9]*\\.?[0-9]+)");

	private boolean isNumeric(String strVal) {
		return strVal != null && numericPattern.matcher(strVal).matches();
	}

	private String strVal(Object value) {
		return value == null ? "" : value.toString();
	}

	public Document getTemplate() throws Exception {
		InputStream stream = this.getClass().getResourceAsStream(
				DOC_TEMPLATE_XML);
		Document document = XmlUtils.loadDocument(stream);
		sheetTemplate = (Element) document.getDocumentElement()
				.getElementsByTagName("Worksheet").item(0);
		sheetTemplate.getParentNode().removeChild(sheetTemplate);
		return document;
	}

	String colnameFromFieldname(String fieldName) {
		String s = fieldName.replace(" ", "_");
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
