/**
 * <p>All these validators should have their 
 * error messages reworked to use the Alicna i18n framework - e.g.:</p>
 * <p><i>(in superclass)</i></p>
 * <pre>
protected String getMessage(String enMsg, Object[] params, String errCode) {
	if (params == null) {
		params = new Object[0];
	}
	String msg = TextProvider.get().getUiObjectText(getClass(),
			errCode, enMsg);
	return CommonUtils.format(msg, params);
}
	</pre>
	<i>in validator</i>
	<pre>
 * getMessage("Value cannot be empty.",null),CNotNullValidator.class);
 * </pre>
 * 
 * @author nick@alcina.cc
 * 
 */
package cc.alcina.framework.common.client.gwittir.validator;

