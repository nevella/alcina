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

class Constants {
	final static int COLOUR_COMPONENT_VALUE = 255;

	final static String PARAMETER = "parameter:";

	final static String FIELD = "field:";

	final static String ASSERT = "assert:";

	final static String HTTP = "http://";

	final static String HTTPS = "https://";

	final static String GET = "GET";

	final static String POST = "POST";

	final static int PORT_NOT_SET = -1;

	final static int UNSECURED_PORT = 80;

	final static int SSL_PORT = 443;

	final static char HOST_PORT_SEPARATOR = ':';

	final static char HOST_OR_PORT_PATH_SEPARATOR = '/';

	final static char PATH_SEPARATOR = '/';

	final static char QUERY_STRING = '?';

	final static char ANCHOR = '#';

	final static char QUERY_PARAMETER_SEPARATOR = '&';

	final static char QUERY_PARAMETER_NAME_VALUE_SEPARATOR = '=';

	final static String QUERY_PARAMETER_SEPARATOR_STRING = ""
			+ QUERY_PARAMETER_SEPARATOR;

	final static String HEADER_NAME_VALUE_SEPARATOR = ": ";

	final static String CONTENT_TYPE_HEADER = "Content-type";

	final static String REFERER_HEADER = "Referer";

	final static String HOST_HEADER = "Host";

	final static String LOCATION_HEADER = "Location";

	final static String CHARACTER_ENCODING = "Character-encoding";

	final static String HTML_MIME_TYPE = "text/html";
}
