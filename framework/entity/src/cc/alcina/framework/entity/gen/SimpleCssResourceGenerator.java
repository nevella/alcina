/*
 * Copyright 2008 Google Inc.
 * 
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
package cc.alcina.framework.entity.gen;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.gen.SimpleCssResource;
import cc.alcina.framework.gwt.client.gen.SimpleCssResource.ResolveParent;

/**
 * Provides implementations of SimpleCssResource.
 */
public final class SimpleCssResourceGenerator extends AbstractResourceGenerator
// FIXME - add caching back
// implements SupportsGeneratorResultCaching {
{
	public static final String IGNORE_DATA_URLS = "alcina.SimpleCssResourceGenerator.ignoreMissingDataUrls";

	/**
	 * Java compiler has a limit of 2^16 bytes for encoding string constants in
	 * a class file. Since the max size of a character is 4 bytes, we'll limit
	 * the number of characters to (2^14 - 1) to fit within one record.
	 */
	private static final int MAX_STRING_CHUNK = 16383;

	private static final int MAX_DATA_URL_LENGTH = 32766;

	boolean logMissingUrlResources = true;

	@Override
	public String createAssignment(TreeLogger logger, ResourceContext context,
			JMethod method) throws UnableToCompleteException {
		try {
			ConfigurationProperty cp = context.getGeneratorContext()
					.getPropertyOracle()
					.getConfigurationProperty(IGNORE_DATA_URLS);
			logMissingUrlResources = !Boolean.valueOf(cp.getValues().get(0));
		} catch (BadPropertyValueException e1) {
			e1.printStackTrace();
		}
		URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
				method);
		if (resources.length != 1) {
			logger.log(TreeLogger.ERROR,
					"Exactly one resource must be specified", null);
			throw new UnableToCompleteException();
		}
		URL resource = resources[0];
		SourceWriter sw = new StringSourceWriter();
		// Write the expression to create the subtype.
		sw.println("new " + SimpleCssResource.class.getName() + "() {");
		sw.indent();
		if (!AbstractResourceGenerator.STRIP_COMMENTS) {
			// Convenience when examining the generated code.
			sw.println("// " + resource.toExternalForm());
		}
		sw.println("public String getText() {");
		sw.indent();
		String toWrite = Util.readURLAsString(resource);
		ResolveParent resolveParent = method.getAnnotation(ResolveParent.class);
		if (context.supportsDataUrls()) {
			try {
				toWrite = replaceWithDataUrls(context, toWrite, resolveParent);
			} catch (Exception e) {
				logger.log(Type.ERROR, "css data url gen", e);
				throw new UnableToCompleteException();
			}
		}
		if (toWrite.length() > MAX_STRING_CHUNK) {
			writeLongString(sw, toWrite);
		} else {
			sw.println("return \"" + Generator.escape(toWrite) + "\";");
		}
		sw.outdent();
		sw.println("}");
		sw.println("public String getName() {");
		sw.indent();
		sw.println("return \"" + method.getName() + "\";");
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		return sw.toString();
	}

	private String[] getAllPublicFiles(ModuleDef module) {
		module.refresh();
		return module.getPublicResourceOracle().getPathNames()
				.toArray(Empty.STRINGS);
	}

	private String replaceWithDataUrls(ResourceContext context, String toWrite,
			ResolveParent resolveParent) throws Exception {
		Pattern urlPat = Pattern
				.compile("url\\s*\\((?!'?data:)(?!http:)(.+?)\\)");
		Matcher m = urlPat.matcher(toWrite);
		while (m.find()) {
			String url = m.group(1);
			int qIdx = url.indexOf('?');
			if (qIdx != -1) {
				url = url.substring(0, qIdx);
			}
			// url = url.replaceFirst("(.+?)\\?.*", "$1");
			url = url.replace("'", "").replace("\"", "");
			StandardGeneratorContext generatorContext = (StandardGeneratorContext) context
					.getGeneratorContext();
			Field compilerContextField = StandardGeneratorContext.class
					.getDeclaredField("compilerContext");
			compilerContextField.setAccessible(true);
			CompilerContext compilerContext = (CompilerContext) compilerContextField
					.get(generatorContext);
			Field moduleField = CompilerContext.class
					.getDeclaredField("module");
			moduleField.setAccessible(true);
			ModuleDef module = (ModuleDef) moduleField.get(compilerContext);
			if (url.startsWith("/")) {
				url = url.substring(1);
			}
			if (url.startsWith("../") && resolveParent != null) {
				url = url.replaceFirst("\\.\\.", resolveParent.value());
			}
			Resource resource = module.findPublicFile(url);
			if (resource == null) {
				resource = module.findPublicFile("gwt/standard/" + url);
			}
			if (resource == null) {
				if (url.contains("://")) {
					continue;
				} else {
					if (logMissingUrlResources) {
						String[] pub = getAllPublicFiles(module);
						// System.out.println("missing url resource - " + url);
						for (String path : pub) {
							if (path.contains(url)) {
								System.out.format("Maybe - %s : %s\n", url,
										path);
							}
						}
					}
					continue;
				}
			}
			InputStream contents = resource.openContents();
			byte[] bytes = ResourceUtilities.readStreamToByteArray(contents);
			String out = Base64.encodeBytes(bytes);
			String fileName = url.replaceFirst(".+/", "");
			String extension = fileName.replaceFirst(".+\\.", "").toLowerCase();
			String mimeType = null;
			if (extension.equals("gif")) {
				mimeType = "image/gif";
			} else if (extension.equals("jpeg")) {
				mimeType = "image/jpeg";
			} else if (extension.equals("jpg")) {
				mimeType = "image/jpeg";
			} else if (extension.equals("png")) {
				mimeType = "image/png";
			} else if (extension.equals("svg")) {
				mimeType = "image/svg+xml";
			} else if (extension.equals("eot")) {
				mimeType = "application/vnd.ms-fontobject";
			} else if (extension.equals("woff")) {
				mimeType = "font/woff";
			} else if (extension.equals("woff2")) {
				mimeType = "font/woff2";
			} else if (extension.equals("ttf")) {
				mimeType = "font/ttf";
			} else {
				throw Ax.runtimeException("unknown mime type: %s", extension);
			}
			if (mimeType != null) {
				String encoded = String.format("url(data:%s;base64,%s)",
						mimeType, out.replace("\n", ""));
				if (encoded.length() > 5000) {
					// System.out.println("warn - large css sprite - " + url);
				}
				// IE8 limitation, ignore
				// if (encoded.length() < MAX_DATA_URL_LENGTH) {
				toWrite = m.replaceFirst(encoded);
				m = urlPat.matcher(toWrite);
				// }else {
				//
				// }
			}
		}
		return toWrite;
	}

	/**
	 * A single constant that is too long will crash the compiler with an out of
	 * memory error. Break up the constant and generate code that appends using
	 * a buffer.
	 */
	private void writeLongString(SourceWriter sw, String toWrite) {
		sw.println("StringBuilder builder = new StringBuilder();");
		int offset = 0;
		int length = toWrite.length();
		while (offset < length - 1) {
			int subLength = Math.min(MAX_STRING_CHUNK, length - offset);
			sw.print("builder.append(\"");
			sw.print(Generator
					.escape(toWrite.substring(offset, offset + subLength)));
			sw.println("\");");
			offset += subLength;
		}
		sw.println("return builder.toString();");
	}
}
