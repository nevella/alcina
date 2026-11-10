/*
 * Copyright 2010 Google Inc.
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
package cc.alcina.framework.common.client.regexp.shared;

/**
 * GWT wrapper for the Javascript RegExp class extended with the Javascript
 * String class's replace and split methods, which can take a RegExp parameter.
 */
public class RegExp_Jso implements IRegExp {
	@Override
	public MatchResult exec(String input) {
		throw new UnsupportedOperationException("Unimplemented method 'exec'");
	}

	@Override
	public boolean getGlobal() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getGlobal'");
	}

	@Override
	public boolean getIgnoreCase() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getIgnoreCase'");
	}

	@Override
	public int getLastIndex() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getLastIndex'");
	}

	@Override
	public boolean getMultiline() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getMultiline'");
	}

	@Override
	public String getSource() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getSource'");
	}

	@Override
	public String replace(String input, String replacement) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'replace'");
	}

	@Override
	public void setLastIndex(int lastIndex) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'setLastIndex'");
	}

	@Override
	public SplitResult split(String input) {
		throw new UnsupportedOperationException("Unimplemented method 'split'");
	}

	@Override
	public SplitResult split(String input, int limit) {
		throw new UnsupportedOperationException("Unimplemented method 'split'");
	}

	@Override
	public boolean test(String input) {
		throw new UnsupportedOperationException("Unimplemented method 'test'");
	}

	public static RegExp compile(String pattern) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'compile'");
	}

	public static String quote(String input) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'quote'");
	}
}
