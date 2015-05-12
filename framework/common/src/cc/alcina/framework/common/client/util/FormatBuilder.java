package cc.alcina.framework.common.client.util;

public class FormatBuilder{
	private StringBuilder sb=new StringBuilder();
	public FormatBuilder format(String template, Object... args){
		sb.append(CommonUtils
				.formatJ(template,args)
				);
		return this;
	}
	@Override
	public String toString() {
		return sb.toString();
	}
	public void println() {
		sb.append("\n");
	}
}