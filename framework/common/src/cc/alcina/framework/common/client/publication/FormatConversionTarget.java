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

import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 * 
 * @author Nick Reddel
 */
public abstract class FormatConversionTarget extends ExtensibleEnum implements
		HasDisplayName {
	public static final FormatConversionTarget DOCX = new FormatConversionTarget_DOCX();

	public static final FormatConversionTarget DOC = new FormatConversionTarget_DOC();

	public static final FormatConversionTarget XLSX = new FormatConversionTarget_XLSX();

	public static final FormatConversionTarget XLS = new FormatConversionTarget_XLS();

	public static final FormatConversionTarget ZIP = new FormatConversionTarget_ZIP();

	public static final FormatConversionTarget EPUB = new FormatConversionTarget_EPUB();

	public static final FormatConversionTarget PDF = new FormatConversionTarget_PDF();

	public static final FormatConversionTarget HTML = new FormatConversionTarget_HTML();

	public static final FormatConversionTarget TEXT = new FormatConversionTarget_TEXT();

	public static final FormatConversionTarget XML = new FormatConversionTarget_XML();

	public static class FormatConversionTarget_DOCX extends
			FormatConversionTarget {
		@Override
		public String displayName() {
			return "Microsoft Word 2007+ (.docx)";
		}
	}

	public static class FormatConversionTarget_DOC extends
			FormatConversionTarget {
		@Override
		public String displayName() {
			return "Microsoft Word (.doc)";
		}
	}

	public static class FormatConversionTarget_XLSX extends
			FormatConversionTarget {
	}

	public static class FormatConversionTarget_XLS extends
			FormatConversionTarget {
	}

	public static class FormatConversionTarget_ZIP extends
			FormatConversionTarget {
	}

	public static class FormatConversionTarget_EPUB extends
			FormatConversionTarget {
		@Override
		public boolean requiresXml() {
			return true;
		}
	}

	public static class FormatConversionTarget_HTML extends
			FormatConversionTarget {
	}

	public static class FormatConversionTarget_PDF extends
			FormatConversionTarget {
		@Override
		public boolean requiresXml() {
			return true;
		}

		@Override
		public String displayName() {
			return "PDF";
		}
	}

	public static class FormatConversionTarget_XML extends
			FormatConversionTarget {
		@Override
		public boolean requiresXml() {
			return true;
		}
	}

	public static class FormatConversionTarget_TEXT extends
			FormatConversionTarget {
	}

	public boolean requiresXml() {
		return false;
	}

	public String displayName() {
		return CommonUtils.friendlyConstant(serializedForm());
	}
}