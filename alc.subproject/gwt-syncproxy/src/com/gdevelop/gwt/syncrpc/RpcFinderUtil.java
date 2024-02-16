/*
 * Copyright www.gdevelop.com.
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
package com.gdevelop.gwt.syncrpc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 * 
 * @author dhartford This is a pretty heavyweight prototype class for use with
 *         AutoSyncProxy, can be improved upon.
 */
public class RpcFinderUtil {
	private static final String NOCACHEJS = ".nocache.js";
	
	public static Map<String,String> getRequestCache=new LinkedHashMap<String, String>();
	private static String getResponseText(String myurl) throws IOException {
		if(getRequestCache.containsKey(myurl)){
			return getRequestCache.get(myurl);
		}
		URL url = new URL(myurl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(true); // follow redirect
		connection.setRequestMethod("GET");
		connection.connect();
		InputStream is = connection.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) > 0) {
			baos.write(buffer, 0, len);
		}
		String responseText = baos.toString("UTF8");
		getRequestCache.put(myurl, responseText);
		return responseText;
	}
	
	public static List<String> guessAllGwtPolicyName(String baseurlwithendingslash, String policyFileName) {
		String url = baseurlwithendingslash + policyFileName;
		String response = "";
		try {
			response = getResponseText(url);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		List<String> findRpcNames = findGwtNames(response);
		List<String> gwtRpcValidatedList = new ArrayList<String>();
		boolean skippedfirst = false;
		for (Iterator iterator = findRpcNames.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			// HERE - THESE ARE THE LIST OF RPC's minus the first one (which is
			// the cache.html file)
			if (skippedfirst) {
				gwtRpcValidatedList.add(string);
			}
			skippedfirst = true;
		}
		return gwtRpcValidatedList;
	}

	public static List<String> guessAllGwtPolicyName(
			String baseurlwithendingslash) {
		
		String[] urlparts = baseurlwithendingslash.split("/");
		String nocachejs = urlparts[urlparts.length - 1] + NOCACHEJS;
		
		String surl = baseurlwithendingslash + nocachejs;
		String responseText = "";
		try {
			responseText = getResponseText(surl);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		// System.out.println(responseText);
		List<String> findGwtNames = findGwtNames(responseText);
		for (Iterator iterator = findGwtNames.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			// System.out.println("top: " + string);
		}
		String firstCacheHtml = findGwtNames.iterator().next();
		String cacheurl = baseurlwithendingslash + firstCacheHtml
				+ ".cache.html";
		String responseCache = "";
		try {
			responseCache = getResponseText(cacheurl);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		List<String> findRpcNames = findGwtNames(responseCache);
		String secondRpcFind = findRpcNames.get(1);
		List<String> gwtRpcValidatedList = new ArrayList<String>();
		boolean skippedfirst = false;
		// System.out.println("second:" + findRpcNames.get(1));
		for (Iterator iterator = findRpcNames.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			// HERE - THESE ARE THE LIST OF RPC's minus the first one (which is
			// the cache.html file)
			if (skippedfirst) {
				gwtRpcValidatedList.add(string);
			}
			skippedfirst = true;
		}
		return gwtRpcValidatedList;
	}

	public static String findServiceName(String rpcUrl) {
		String result = null;
		String responseText = null;
		try {
			responseText = getResponseText(rpcUrl);
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		BufferedReader reader;
		try {
			reader = new BufferedReader(new java.io.StringReader(responseText));
			String line = reader.readLine();
			while (line != null) {
				int pos = line.indexOf(", false, false, false, false, _, ");
				if (pos > 0) {
					// String policyName = child.substring(0, child.length() -
					// GWT_PRC_POLICY_FILE_EXT.length());
					result = line.substring(0, pos);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			// ignore
		}
		return result;
	}

	public static SerializationPolicy getSchedulePolicy(String rpcUrl) {
		SerializationPolicy result = null;
		String responseText = null;
		try {
			responseText = getResponseText(rpcUrl);
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		InputStream is = new java.io.StringBufferInputStream(responseText);
		try {
			result = SerializationPolicyLoader.loadFromStream(is, null);
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (ParseException e) {
			
			e.printStackTrace();
		}
		return result;
	}

	private static List<String> findGwtNames(String responseText) {
		List<String> result = new ArrayList<String>();
		// 32 chars surrounded by apostrophe
		String regex = "\'([A-Z0-9]){32}\'";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(responseText);
		while (matcher.find()) {
			String temp = matcher.group();
			temp = temp.replace("\'", "");
			result.add(temp);
			// System.out.println("I found the text " + matcher.group() +
			// " starting at index " + matcher.start() + " and ending at "
			// +matcher.end() );
		}
		return result;
	}
}