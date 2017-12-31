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
package cc.alcina.framework.common.client.publication;

import java.io.Serializable;
import java.util.List;

/**
 * Marker interface for second stage of the publication process
 * 
 * @author nick@alcina.cc
 *
 *         Note - these will want to be xmlroot elt, jaxb registered, because
 *         they will be jaxb-serialised for transform
 */
public interface PublicationContent extends Serializable {
	default String getCss() {
		return "";
	}

	default List getGridRows() {
		return null;
	}
}
