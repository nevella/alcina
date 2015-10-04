package cc.alcina.framework.common.client.util;

public class FormatBuilder{
	private StringBuilder sb=new StringBuilder();
	public FormatBuilder line(String template, Object... args){
		return format(template, args).newLine();
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
	public FormatBuilder newLine() {
		sb.append("\n");
		return this;
	}
	public void appendIfNonEmpty(String optional) {
		if(sb.length()>0){
			sb.append(optional);
		}
	}
}