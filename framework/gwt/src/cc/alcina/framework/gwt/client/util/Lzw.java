package cc.alcina.framework.gwt.client.util;

import com.google.gwt.core.client.JavaScriptObject;

public class Lzw {
	public boolean checkRoundtrip(String data) {
		JavaScriptObject encoder = createEncoder();
		return checkRoundtrip0(encoder, data);
	}

	public String compress(String data) {
		JavaScriptObject encoder = createEncoder();
		String encoded = encode(encoder, data);
		return encoded;
	}

	public String decompress(String data) {
		JavaScriptObject encoder = createEncoder();
		String decoded = decode(encoder, data);
		return decoded;
	}

	private native String encode(JavaScriptObject encoder, String data) /*-{
																		return encoder.compressToUtf16Str(data);
																		}-*/;

	native boolean checkRoundtrip0(JavaScriptObject encoder,
			String data) /*-{
							var arr = encoder.compress(data);
							var s2 = encoder.decompress(arr);
							return data == s2;
							}-*/;

	/**
	 * Could have done something fancy with utf8 - but given target string is
	 * sqllite/utf-16, don't bother
	 * 
	 * !!Note the use of arr.hasOwnProperty(instead of arr[]) - otherwise words
	 * like 'pop', 'push' and other array methods will cause tricky corruption
	 */
	native JavaScriptObject createEncoder()/*-{
											//http://rosettacode.org/wiki/LZW_compression#JavaScript
											var LZW = {
											byteArrToUtf16Str : function(arr) {
											//1=odd,0=even
											var result = new String(arr.length % 2);
											for ( var i = 0; i < arr.length; i += 2) {
											var v = arr[i];
											if (i + 1 < arr.length) {
											v += arr[i + 1] << 8;
											}
											result += String.fromCharCode(v);
											}
											return result;
											},
											utf16StrToByteArr : function(str) {
											var even = str.charAt(0) == "0";
											var result = [];
											for ( var i = 1; i < str.length; i++) {
											var c = str.charCodeAt(i);
											result.push(c & 255);
											if ((i + 1 < str.length) || even) {
											result.push(c >> 8);
											}
											}
											return result;
											},
											intArrToUtf16Str : function(arr) {
											var result = "";
											for ( var i = 0; i < arr.length; i++) {
											result += String.fromCharCode(arr[i]);
											}
											return result;
											},
											utf16StrToIntArr : function(str) {
											var even = str.charAt(0) == "0";
											var result = [];
											for ( var i = 0; i < str.length; i++) {
											result.push(str.charCodeAt(i));
											}
											return result;
											},
											compressToUtf16Str : function(uncompressed) {
											var arr = this.compress(uncompressed);
											return this.intArrToUtf16Str(arr);
											},
											decompressUtf16Str : function(compressed) {
											var arr = this.utf16StrToIntArr(compressed);
											return this.decompress(arr);
											},
											maxDictSize : 4096,
											compress : function(uncompressed) {
											// Build the dictionary.
											var i, dictionary = null, c, wc, w = "", result = [], dictSize = 256;
											
											for (i = 0; i < uncompressed.length; i += 1) {
											var reset = (dictSize == this.maxDictSize);
											if (dictionary == null || reset) {
											dictionary = [];
											for (dictSize = 0; dictSize < 256; dictSize += 1) {
											dictionary[String.fromCharCode(dictSize)] = dictSize;
											}
											}
											c = uncompressed.charAt(i);
											wc = w + c;
											if (dictionary.hasOwnProperty(wc)) {
											w = wc;
											} else {
											result.push(dictionary[w]);
											// Add wc to the dictionary.
											dictionary[wc] = dictSize++;
											w = String(c);
											}
											}
											
											// Output the code for w.
											if (w !== "") {
											result.push(dictionary[w]);
											}
											return result;
											},
											
											decompress : function(compressed) {
											// Build the dictionary.
											var i, j, dictionary = null, w, result, k, entry = "", dictSize = 256;
											
											w = String.fromCharCode(compressed[0]);
											result = w;
											for (i = 1; i < compressed.length; i += 1) {
											k = compressed[i];
											var reset = (dictSize == this.maxDictSize);
											if (dictionary == null || reset) {
											dictionary = [];
											for (dictSize = 0; dictSize < 256; dictSize += 1) {
											dictionary[dictSize] = String.fromCharCode(dictSize);
											}
											}
											if (dictionary[k]) {
											entry = dictionary[k];
											} else {
											if (k === dictSize) {
											entry = w + w.charAt(0);
											} else {
											return null;
											}
											}
											
											result += entry;
											
											// Add w+entry[0] to the dictionary.
											dictionary[dictSize++] = w + entry.charAt(0);
											
											w = entry;
											}
											return result;
											}
											}
											return LZW;
											}-*/;

	native String decode(JavaScriptObject encoder, String data) /*-{
																return encoder.decompressUtf16Str(data);
																}-*/;
}
