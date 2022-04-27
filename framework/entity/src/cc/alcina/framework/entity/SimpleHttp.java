package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;

/**
 * Helper class to make web requests of various kinds
 */ 
public class SimpleHttp {
	// URL to request
	private String strUrl;

	// Body for POST and PUT requests
	private String body;

	// Headers to add to request
	private StringMap headers = new StringMap();

	// Internal connection handle
	private HttpURLConnection connection;

	// Request is gzipped
	private boolean gzip;

	// Decode GZIP responses
	private boolean decodeGz;

	// Content type of response
	private String contentType;

	// Content-Disposition field of response
	private String contentDisposition;

	// Method to request
	private String method = "GET";

	// Response code of response
	private int responseCode;

	// Query string parameters for request
	private StringMap queryStringParameters;

	// Throw on response codes >= 400
	private boolean throwOnResponseCode = true;

	// Timeout for reading/connecting 
	private int timeout = 0;

	// Add a custom HostnameVerifier when making the request
	private HostnameVerifier hostnameVerifier;

	public SimpleHttp(String strUrl) {
		this.strUrl = strUrl;
	}

	// Request the URL and return as bytes
	public byte[] asBytes() throws Exception {
		InputStream in = null;
		connection = null;
		// Ensure headers are present
		if (headers == null) {
			headers = new StringMap();
		}
		// Generate query string if present and add to request URL
		if (queryStringParameters != null) {
			if (!strUrl.contains("?")) {
				strUrl += "?";
			}
			strUrl += queryStringParameters.entrySet().stream().map(e -> {
				return Ax.format("%s=%s", e.getKey(),
						UrlComponentEncoder.get().encode(e.getValue()));
			}).collect(Collectors.joining("&"));
		}
		try {
			// Setup connection to remote
			URL url = new URL(strUrl);
			connection = (HttpURLConnection) (url.openConnection());
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			// If there's a custom timeout, set it on the connection
			if (timeout > 0) {
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
			}
			// If custom HostnameVerifier present, set on connection
			if (hostnameVerifier != null
					&& connection instanceof HttpsURLConnection) {
				((HttpsURLConnection) connection)
						.setHostnameVerifier(hostnameVerifier);
			}
			// Set response method
			connection.setRequestMethod(method);
			// Add GZIP to accepted encoding if accepted
			if (gzip) {
				headers.put("accept-encoding", "gzip");
			}
			// Add each header to connection
			for (Entry<String, String> e : headers.entrySet()) {
				connection.setRequestProperty(e.getKey(), e.getValue());
			}
			// If body is present, send to remote
			if (body != null) {
				OutputStream out = connection.getOutputStream();
				Writer wout = new OutputStreamWriter(out, "UTF-8");
				wout.write(body);
				wout.flush();
				wout.close();
			}
			// Read response
			byte[] respBytes = null;
			responseCode = connection.getResponseCode();
			// If the response code is bad, read the error stream instead
			if (responseCode >= 400) {
				InputStream err = connection.getErrorStream();
				if (err != null) {
					respBytes = ResourceUtilities.readStreamToByteArray(err);
				}
				// If throwOnResponseCode and we get a 4xx or 5xx code
				// throw a IOException
				if (throwOnResponseCode) {
					// Read the error string if available
					String errString = null;
					if (respBytes != null) {
						// If decodeGz, decode the gzipped data
						if (decodeGz) {
							respBytes = maybeDecodeGzip(respBytes);
						}
						// Read the error into a string
						errString = new String(respBytes,
								StandardCharsets.UTF_8);
					}
					// For backwards compat, read the stream anyway to get
					// the error
					// Store the IOException returned, then throw it with
					// the decoded string
					IOException ioe = null;
					try {
						in = connection.getInputStream();
					} catch (IOException e) {
						ioe = e;
					}
					// Throw with errString if available, otherwise just
					// throw
					if (errString != null) {
						throw new IOException(errString, ioe);
					} else {
						throw new IOException(ioe);
					}
				}
			} else {
				in = connection.getInputStream();
				// If code is good, read the stream normally
				respBytes = ResourceUtilities.readStreamToByteArray(in);
			}
			// If decodeGz and bytes are non-null, decode the gzipped data
			if (decodeGz && respBytes != null) {
				respBytes = maybeDecodeGzip(respBytes);
			}
			// Store some other response data to the class
			contentType = connection.getContentType();
			contentDisposition = connection
					.getHeaderField("Content-Disposition");
			// Return byte arrays
			return respBytes;
		} finally {
			// Ensure streams and connection are closed
			if (in != null) {
				in.close();
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	// Set as a PUT request
	public SimpleHttp withPutMethod() {
		this.method = "PUT";
		return this;
	}

	// Request the URL and return as string
	public String asString() throws Exception {
		byte[] respBytes = asBytes();
		if (respBytes != null) {
			return new String(respBytes, StandardCharsets.UTF_8);
		} else {
			return null;
		}
	}

	// Request the URL and print the response string
	public void echo() {
		try {
			Ax.out(asString());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// Get Content-Disposition header value from response
	public String getContentDisposition() {
		return this.contentDisposition;
	}

	// Get Content-Type header value from response
	public String getContentType() {
		return this.contentType;
	}

	// Get status code of the response
	public int getResponseCode() {
		return this.responseCode;
	}

	// Set Authorization header with basic authentication pair
	public SimpleHttp withBasicAuthentication(String username,
			String password) {
		String auth = Ax.format("%s:%s", username, password);
		headers.put("Authorization",
				Ax.format("Basic %s", Base64.getEncoder().encodeToString(
						auth.getBytes(StandardCharsets.UTF_8))));
		return this;
	}

	// Set Authorization header with bearer token
	public SimpleHttp withBearerAuthentication(String token) {
		headers.put("Authorization", Ax.format("Bearer %s", token));
		return this;
	}

	// Set body for the request
	public SimpleHttp withBody(String body) {
		this.body = body;
		return this;
	}

	// Set Content-Type header to given string
	public SimpleHttp withContentType(String string) {
		headers.put("content-type", string);
		return this;
	}

	// Set whether to decode on gzipped response
	public SimpleHttp withDecodeGz(boolean decodeGz) {
		this.decodeGz = decodeGz;
		return this;
	}

	// Set whether to accept a gzipped response
	public SimpleHttp withGzip(boolean gzip) {
		this.gzip = gzip;
		return this;
	}

	// Set headers on request
	public SimpleHttp withHeaders(StringMap headers) {
		this.headers = headers;
		return this;
	}

	// Set custom HostnameVerifier for the request
	public SimpleHttp
			withHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
		return this;
	}

	// Set method for the request
	public SimpleHttp withMethod(String method) {
		this.method = method;
		return this;
	}

	// Set to a POST request, with given body
	public SimpleHttp withPostBody(String postBody) {
		this.method = "POST";
		this.body = postBody;
		return this;
	}

	// Set to a POST request with given query params as body
	public SimpleHttp
			withPostBodyQueryParameters(StringMap queryParameters) {
		this.method = "POST";
		this.body = queryParameters.entrySet().stream().map(e -> {
			return Ax.format("%s=%s", e.getKey(),
					UrlComponentEncoder.get().encode(e.getValue()));
		}).collect(Collectors.joining("&"));
		headers.put("content-type", "application/x-www-form-urlencoded");
		return this;
	}

	// Set query string parameters for the request
	public SimpleHttp
			withQueryStringParameters(StringMap queryStringParameters) {
		this.queryStringParameters = queryStringParameters;
		return this;
	}

	// Set whether to throw on a 4xx or 5xx response code
	public SimpleHttp
			withThrowOnResponseCode(boolean throwOnResponseCode) {
		this.throwOnResponseCode = throwOnResponseCode;
		return this;
	}

	/**
	 * Set read/connect timeout for this request
	 * @param timeout Timeout to set
	 * @return this SimpleHttp object
	 */
	public SimpleHttp withTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	// If the request reports a Content-Encoding of gzip, decode request as
	// gzip
	// otherwise, just return the input as is
	private byte[] maybeDecodeGzip(byte[] input) throws IOException {
		if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
			return ResourceUtilities.readStreamToByteArray(
					new GZIPInputStream(new ByteArrayInputStream(input)));
		} else {
			return input;
		}
	}
}