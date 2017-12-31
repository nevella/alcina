package cc.alcina.framework.entity;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

import cc.alcina.framework.common.client.util.CachingMap;

public class XpathAndExpressionCache {
	XPath xPath;

	CachingMap<String, XPathExpression> expressionCache = new CachingMap<String, XPathExpression>(
			s -> xPath.compile(s));

	public XpathAndExpressionCache(XPath xPath) {
		this.xPath = xPath;
	}
}