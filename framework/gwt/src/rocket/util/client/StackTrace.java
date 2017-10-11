/*
 * Copyright Miroslav Pokorny
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
package rocket.util.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class helps with converting stack traces - regardless of source into a
 * string that is similar to the familiar java stacktrace string produced by
 * {@link java.lang.Throwable#printStackTrace() }.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class StackTrace {
	/**
	 * Converts the stackTrace of the given Throwable into a String that looks
	 * similar to a printed stackTrace.
	 * 
	 * ThrowableClassName at className.methodName.level0 at
	 * className.methodName.level1
	 * 
	 * @param throwable
	 * @return
	 */
	public static String asString(final Throwable throwable) {
		Checker.notNull("parameter:throwable ", throwable);
		final StringBuffer buf = new StringBuffer();
		buf.append(throwable.getClass().getName());
		buf.append(':');
		final String message = throwable.getMessage();
		if (false == Tester.isNullOrEmpty(message)) {
			buf.append(' ');
			buf.append(throwable.getMessage());
		}
		buf.append('\n');
		final StackTraceElement[] elements = throwable.getStackTrace();
		for (int i = 0; i < elements.length; i++) {
			buf.append("\tat ");
			final StackTraceElement element = elements[i];
			buf.append(element.getClassName());
			final String methodName = element.getMethodName();
			if (methodName.length() > 0) {
				buf.append('.');
				buf.append(methodName);
			}
			buf.append('(');
			String fileName = element.getFileName();
			if (null == fileName) {
				fileName = "unknown";
			}
			buf.append(fileName);
			final int lineNumber = element.getLineNumber();
			if (false == (fileName.equals("unknown") || fileName
					.equals("native"))) {
				if (-1 != lineNumber) {
					buf.append(':').append(lineNumber);
				}
			}
			buf.append(")\n");
		}
		return buf.toString();
	}

	/**
	 * Returns as an array the names of all the functions that make up the
	 * current callstack.
	 * 
	 * @return
	 */
	public static String[] getCallStackFunctionNames(
			final JavaScriptObject callStackFunctions) {
		Checker.notNull("parameter:callStackFunctions", callStackFunctions);
		final String functionNames = getCallStackFunctionNames0(callStackFunctions);
		return Utilities.split(functionNames, ",", true);
	}

	native private static String getCallStackFunctionNames0(
			final JavaScriptObject functions)/*-{
												var names = "";

												for( var i = 0; i < functions.length; i++ ){
												var s = functions[ i ];
												var n = null;

												while( true ){
												if( s.name ){
												n = s.name;
												break;
												}

												n = s.toString().match(/function ([$\w]*)/);
												if( ! n ){
												n = "anonymous";
												break;
												}

												n = n[ 1 ];
												if( ! n ){
												n = "anonymous";
												break;
												}

												break;
												}
												if( names.length > 0 ){
												names = names + ",";
												}
												names = names + n;
												}
												return names;
												}-*/;

	/**
	 * Builds and returns an array of function objects for each level of the
	 * callstack.
	 * 
	 * If the javascript host is within some sort of nested recursive stack
	 * parts below the repeated stack will be lost.
	 * 
	 * @return
	 */
	native public static JavaScriptObject getCallStackFunctions() /*-{
																	var elements = [];
																	var i =  0;
																	var context = @rocket.util.client.StackTrace::getCallStackFunctions();

																	while( true ){
																	elements[ i++] = context;

																	var parent = context.caller;
																	if( ! parent ){
																	break;
																	}
																	// special check to determine if in some sort of nested recursive function.
																	//for( var j = 0; j < elements.length; j++ ){

																	// search the array backwards - if we are stuck in a recursive loop then the same function will the last element in the array
																	var j = elements.length;
																	while( j >= 0 ){
																	j--;
																	if( parent == elements[ j ]){
																	parent = null;
																	break;
																	}
																	}
																	if( ! parent ){
																	break;
																	}
																	context = parent;            
																	}
																	return elements;        
																	}-*/;

	/**
	 * Builds an array of StackTraceElements with each individual element
	 * containing the converted functionNames.
	 * 
	 * This method handles converted functionNames outputted in prettyMode.
	 * 
	 * @param functionNames
	 * @return
	 */
	public static StackTraceElement[] buildStackTraceElements(
			final String[] functionNames) {
		Checker.notNull("parameter:functionNames", functionNames);
		final int count = functionNames.length;
		final StackTraceElement[] elements = new StackTraceElement[count];
		for (int i = 0; i < count; i++) {
			String declaringClass = null;
			String methodName = null;
			String fileName = "";
			int lineNumber = -1;
			String arguments = "";
			while (true) {
				String functionName = functionNames[i];
				fileName = functionName;
				// special test for unnamed or anonymous methods...
				if (functionName.equals("anonymous")) {
					declaringClass = "anonymous";
					methodName = "";
					arguments = "unknown";
					break;
				}
				int argumentStartIndex = functionName.indexOf("__");
				// if $argumentStartIndex was not found the name does not appear
				// to be a class/method compound name.
				if (-1 == argumentStartIndex) {
					declaringClass = functionName;
					methodName = "";
					// try again to split into className/methodName...
					final int classNameStart = functionName.lastIndexOf('_');
					if (-1 == classNameStart) {
						break;
					}
					// check that the className begins with a Capital letter.
					final char firstLetterOfClassName = functionName
							.charAt(classNameStart + 1);
					if (Character.toUpperCase(firstLetterOfClassName) != firstLetterOfClassName) {
						break;
					}
					// convert _ into <dots>
					declaringClass = functionName.replace('_', '.');
					methodName = "";
					arguments = "";
					break;
				}
				// sometimes three _ separate class/method from the argument
				// list...
				argumentStartIndex = argumentStartIndex + 2;
				if (argumentStartIndex < functionName.length()
						&& functionName.charAt(argumentStartIndex) == '_') {
					argumentStartIndex++;
				}
				// convert _ into <dots>
				final String convertedFunctionName = functionName.replace('_',
						'.');
				// separate $functionName into className/method/arguments.
				final String classAndMethodNameCompound = convertedFunctionName
						.substring(0, argumentStartIndex - 2);
				int methodNameStart = classAndMethodNameCompound
						.lastIndexOf('.');
				// by chance it appears the functionName had a '_' but no
				// class/method name.(trailing __)
				if (-1 == methodNameStart) {
					declaringClass = functionName;
					methodName = "";
					arguments = "unknown";
					break;
				}
				declaringClass = classAndMethodNameCompound.substring(0,
						methodNameStart);
				// if methodName starts with a $ skip it.
				final boolean staticMethod = '$' == classAndMethodNameCompound
						.charAt(methodNameStart + 1);
				if (staticMethod) {
					methodNameStart++;
				}
				methodName = classAndMethodNameCompound
						.substring(methodNameStart + 1);
				// special test for static initializers.
				if (staticMethod && methodName.equals("clint")) {
					methodName = "<clint>";
					arguments = "";
					break;
				}
				// check for arguments...
				if (convertedFunctionName.length() == argumentStartIndex) {
					break;
				}
				String unconvertedArguments = convertedFunctionName
						.substring(argumentStartIndex);
				final StringBuffer convertedArguments = new StringBuffer();
				int j = 0;
				while (j < unconvertedArguments.length()) {
					final char c = unconvertedArguments.charAt(j++);
					String typeName = null;
					while (true) {
						if (c == '[') {
							typeName = "[]";
							break;
						}
						if (c == 'L') {
							// Ljava_lang_Object_2
							int endOfTypeName = unconvertedArguments.indexOf(
									".2", j + 1);
							if (-1 == endOfTypeName) {
								endOfTypeName = unconvertedArguments.length();
							}
							typeName = unconvertedArguments.substring(j,
									endOfTypeName);
							j = endOfTypeName + 2;
							break;
						}
						if (c == 'Z') {
							typeName = "boolean";
							break;
						}
						if (c == 'B') {
							typeName = "byte";
							break;
						}
						if (c == 'C') {
							typeName = "char";
							break;
						}
						if (c == 'S') {
							typeName = "short";
							break;
						}
						if (c == 'I') {
							typeName = "int";
							break;
						}
						if (c == 'J') {
							typeName = "long";
							break;
						}
						if (c == 'F') {
							typeName = "float";
							break;
						}
						if (c == 'D') {
							typeName = "double";
							break;
						}
						typeName = "?";
						j = Integer.MAX_VALUE;
						break;
					}
					convertedArguments.append(typeName);
					if (j < unconvertedArguments.length()) {
						convertedArguments.append(',');
					}
				}
				arguments = convertedArguments.toString();
				break;
			}
			// constructors will have the same name as their class... test for
			// this and blank methodName if this is the case.
			if (declaringClass.endsWith(methodName)) {
				methodName = "";
			}
			if (declaringClass.length() == 0) {
				final String swap = methodName;
				methodName = declaringClass;
				declaringClass = swap;
			}
			fileName = arguments;
			elements[i] = new StackTraceElement(declaringClass, methodName,
					fileName, lineNumber);
		}
		return elements;
	}

	protected static String[] getFunctionNames(final JavaScriptObject context) {
		Checker.notNull("parameter:context", context);
		final String functionNames = StackTrace.getFunctionNames0(context);
		return Utilities.split(functionNames, ",", true);
	}

	/**
	 * This method visits the stacktrace within the given context object.
	 * 
	 * @param context
	 *            The context object that contains the stacktrace.
	 * @return A string containing the function names each separated by commas
	 */
	native private static String getFunctionNames0(
			final JavaScriptObject context)/*-{
											// this array will be filled with the names of each level of the stack trace...
											var names = "";
											var addSeparator = false;

											while( context ){
											var name = context.name | context.callee;
											// is name is missing assign "anonymous"
											if( ! name ){
											name = "anonymous";
											}                
											if( addSeparator ){
											names = names + ",";
											}

											names = names + name;
											addSeparator = true;

											// if function call is the result of a recursive call stop looping. 
											var parent = context.caller;
											if( context == parent ){
											break;
											}
											context = parent; 
											}

											return names;
											}-*/;
}
