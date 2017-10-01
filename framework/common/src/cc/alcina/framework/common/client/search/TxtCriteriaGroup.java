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
import cc.alcina.framework.common.client.logic.reflection.Bean;

@Bean(displayNamePropertyName = "displayName")
/**
 *
 * @author Nick Reddel
 */
@PermissibleChildClasses({ TxtCriterion.class })
public class TxtCriteriaGroup extends CriteriaGroup<TxtCriterion> {

    static final transient long serialVersionUID = -1L;

    private String displayName = "Text";

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TxtCriteriaGroup() {
        super();
    }

    public TxtCriteriaGroup(String displayName) {
        this();
        TxtCriterion tc = new TxtCriterion();
        tc.setDisplayName(displayName);
        setDisplayName(displayName);
        getCriteria().add(tc);
    }

    @Override
    public Class getEntityClass() {
        return null;
    }

    /**
	 * for multiple tcgs, mapping to different properties
	 *
	 * @author nick@alcina.cc
	 *
	 */
    public static class TxtCriteriaGroup2 extends TxtCriteriaGroup {

        public TxtCriteriaGroup2() {
            super();
        }

        public TxtCriteriaGroup2(String displayName) {
            super(displayName);
        }
    }

    public static class TxtCriteriaGroup3 extends TxtCriteriaGroup {

        public TxtCriteriaGroup3() {
            super();
        }

        public TxtCriteriaGroup3(String displayName) {
            super(displayName);
        }
    }
}
