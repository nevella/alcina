package cc.alcina.framework.servlet.sync;

import com.totsp.gwittir.client.beans.Converter;

public class SyncInfoConversionSpec {
	private String name1;

	private String name2;

	private Converter converter;

	public String getName1() {
		return this.name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}

	public String getName2() {
		return this.name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}

	public Converter getConverter() {
		return this.converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}
}
