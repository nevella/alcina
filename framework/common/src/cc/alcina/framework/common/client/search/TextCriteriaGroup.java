/*
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
package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/**
 *
 * @author Nick Reddel
 */
@PermissibleChildClasses({ TextCriterion.class })
@TypeSerialization(flatSerializable = false)
public class TextCriteriaGroup extends CriteriaGroup<TextCriterion> {
	private String displayName = "Text";

	public TextCriteriaGroup() {
		super();
	}

	public TextCriteriaGroup(String displayName) {
		this();
		TextCriterion tc = new TextCriterion();
		tc.setDisplayName(displayName);
		setDisplayName(displayName);
		getCriteria().add(tc);
	}

	@Override
	public Class entityClass() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * for multiple tcgs, mapping to different properties
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class TextCriteriaGroup2 extends TextCriteriaGroup {
		public TextCriteriaGroup2() {
			super();
		}

		public TextCriteriaGroup2(String displayName) {
			super(displayName);
		}
	}

	public static class TextCriteriaGroup3 extends TextCriteriaGroup {
		public TextCriteriaGroup3() {
			super();
		}

		public TextCriteriaGroup3(String displayName) {
			super(displayName);
		}
	}
}
