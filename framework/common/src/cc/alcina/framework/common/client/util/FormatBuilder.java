package cc.alcina.framework.common.client.util;

public class FormatBuilder{
	private StringBuilder sb=new StringBuilder();
	public FormatBuilder line(String template, Object... args){
		return format(template, args).println();
	}
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
	public FormatBuilder println() {
		sb.append("\n");
		return this;
	}
}