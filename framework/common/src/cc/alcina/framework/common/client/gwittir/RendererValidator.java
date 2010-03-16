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

package cc.alcina.framework.common.client.gwittir;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

/**
 *
 * @author Nick Reddel
 */

 public class RendererValidator implements Validator {
	private final Renderer renderer;

	public RendererValidator(Renderer renderer) {
		this.renderer = renderer;
	}


	public Object validate(Object value) throws ValidationException {
		return renderer.render(value);
	}
}
