package cc.alcina.framework.servlet.environment;

import java.util.List;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.util.source.SourceNodes;
import cc.alcina.framework.entity.util.source.SourceNodes.SourceMethod;

/**
 * Builds a javascript block from a JSNI method and an argument list
 */
class JsInvokeBuilder {
	String build(Class clazz, String methodName, List<Class> argumentTypes,
			List<?> arguments) {
		SourceMethod method = SourceNodes.getMethod(clazz, methodName,
				argumentTypes);
		argumentTypes = method.getArgumentTypes();
		FormatBuilder builder = new FormatBuilder();
		for (int idx = 0; idx < argumentTypes.size(); idx++) {
			builder.line("var %s = %s;", method.argumentNames.get(idx),
					jsLiteral(argumentTypes.get(idx), arguments.get(idx)));
		}
		builder.line(method.getBody());
		String script = builder.toString();
		return script;
	}

	String jsLiteral(Class type, Object value) {
		return ReflectiveSerializer.toJavascriptLiteral(value);
	}
}
