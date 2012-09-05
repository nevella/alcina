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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

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
	public void addCollectionToBook(Collection coll, Document book,
			String sheetName) throws Exception {
		if (!coll.iterator().hasNext()) {
			return;
		}
		Object o = coll.iterator().next();
		Class clazz = o.getClass();
		BeanInfo beanInfo = ResourceUtilities.getBeanInfo(clazz);
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		 addCollectionToBook(coll, book, sheetName, pds);
	}
	public void addCollectionToBook(Collection coll, Document book,
			String sheetName,PropertyDescriptor[] pds) throws Exception {
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
				String.valueOf(pds.length));
		table.setAttributeNS(SS_NS, "ss:ExpandedRowCount",
				String.valueOf(coll.size() + 1));
		Element row;
		Element cell;
		Element data;
		Text txt;
		row = book.createElement("Row");
		row.setAttributeNS(SS_NS, "ss:StyleID", "s23");
		for (PropertyDescriptor pd : pds) {
			Method m = pd.getReadMethod();
			ExcelFormatAnnotation ann = m
					.getAnnotation(ExcelFormatAnnotation.class);
			Element col = book.createElement("Column");
			col.setAttributeNS(SS_NS, "ss:AutoFitWidth", "0");
			col.setAttributeNS(SS_NS, "ss:Width", "120");
			if (ann != null && ann.type() == ExcelDatatype.DateTime) {
				col.setAttributeNS(SS_NS, "ss:StyleID", "sDate");
			}
			table.appendChild(col);
			cell = book.createElement("Cell");
			data = book.createElement("Data");
			row.appendChild(cell);
			cell.appendChild(data);
			data.setAttributeNS(SS_NS, "ss:Type", "String");
			txt = book
					.createTextNode(colnameFromFieldname(pd.getDisplayName()));
			data.appendChild(txt);
		}
		table.appendChild(row);
		for (Iterator it = coll.iterator(); it.hasNext();) {
			o = it.next();
			row = book.createElement("Row");
			for (PropertyDescriptor pd : pds) {
				cell = book.createElement("Cell");
				data = book.createElement("Data");
				row.appendChild(cell);
				cell.appendChild(data);
				Method m = pd.getReadMethod();
				ExcelFormatAnnotation ann = m
						.getAnnotation(ExcelFormatAnnotation.class);
				data.setAttributeNS(SS_NS, "ss:Type", (ann == null) ? "String"
						: ann.type().toString());
				Object val = m.invoke(o, CommonUtils.EMPTY_OBJECT_ARRAY);
				txt = book.createTextNode((val == null) ? "" : val.toString());
				data.appendChild(txt);
			}
			table.appendChild(row);
		}
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
		row.setAttributeNS(SS_NS, "ss:StyleID", "s23");
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
			for (Iterator itr = rowCollection.iterator(); itr.hasNext();) {
				cell = book.createElement("Cell");
				data = book.createElement("Data");
				row.appendChild(cell);
				cell.appendChild(data);
				Object value = itr.next();
				String strVal = strVal(value);
				data.setAttributeNS(SS_NS, "ss:Type",
						isNumeric(strVal) ? "Number" : "String");
				Matcher m = percentPattern.matcher(strVal);
				if (m.matches()) {
					Double v2 = Double.parseDouble(m.group(1)) / 100;
					strVal=v2.toString();
					data.setAttributeNS(SS_NS, "ss:Type","Number");
					cell.setAttributeNS(SS_NS, "ss:StyleID", "sPercent");
				}
				
				txt = book.createTextNode(strVal);
				data.appendChild(txt);
			}
			table.appendChild(row);
		}
	}
	private Pattern percentPattern = Pattern.compile("([0-9.]+)%");
	private Pattern numericPattern = Pattern.compile("[0-9.]+");

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
