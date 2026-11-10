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
 * GWT wrapper for Javascript RegExp matching results.
 */
public class MatchResult_Jso implements IMatchResult {
	@Override
	public String getGroup(int index) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getGroup'");
	}

	@Override
	public int getGroupCount() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getGroupCount'");
	}

	@Override
	public int getIndex() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getIndex'");
	}

	@Override
	public String getInput() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getInput'");
	}
}
