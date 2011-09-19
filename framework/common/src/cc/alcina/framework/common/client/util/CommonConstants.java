package cc.alcina.framework.common.client.util;

public class CommonConstants {
	public static final String HTML_BLOCKS = ",ADDRESS,BLOCKQUOTE,DIV,DL,H1,H2,H3,H4,H5,"
			+ "H6,IFRAME,ILAYER,LAYER,OL,TABLE,TR,UL,TD,P,HR,BR,LI,CENTER,PRE,";

	public static final String HTML_TOPS = ",HEAD,HTML,BODY,";

	public static final String HTML_NO_CONTENT = ",BR,HR,INPUT,";

	public static final String HTML_INVISIBLE_CONTENT_ELEMENTS = ",HEAD,STYLE,TEXTAREA,SCRIPT,INPUT,SELECT,";

	/**
	 * note - question is "is a child indented"...an element never indents
	 * itself
	 */
	public static final String HTML_INDENTS = ",BLOCKQUOTE,UL,OL,";

	public static final String HTML_SELF_OUTDENTS = ",LI,";
}
