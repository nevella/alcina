package cc.alcina.framework.servlet.sync;

import com.totsp.gwittir.client.beans.Converter;

public class SyncConversionSpec {
	private String left;

	private String right;

	private Converter leftToRight;

	private Converter rightToLeft;
	
	private Boolean leftOverrides;
	
	private Boolean rightOverrides;

	public SyncConversionSpec() {
	}

	public SyncConversionSpec(String left, String right) {
		this.left = left;
		this.right = right;
	}

	public String getLeft() {
		return this.left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getRight() {
		return this.right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public Converter getLeftToRight() {
		return this.leftToRight;
	}

	public void setLeftToRight(Converter converter) {
		this.leftToRight = converter;
	}

	public Converter getRightToLeft() {
		return this.rightToLeft;
	}

	public void setRightToLeft(Converter reverseConverter) {
		this.rightToLeft = reverseConverter;
	}

	public Boolean getLeftOverrides() {
		return this.leftOverrides;
	}

	public void setLeftOverrides(Boolean leftOverrides) {
		this.leftOverrides = leftOverrides;
	}

	public Boolean getRightOverrides() {
		return this.rightOverrides;
	}

	public void setRightOverrides(Boolean rightOverrides) {
		this.rightOverrides = rightOverrides;
	}
}
