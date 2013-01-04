package cc.alcina.framework.servlet.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LzwJvm {
	static int maxDictSize = 4096;

	public static String intArrToUtf16Str(List<Integer> arr) {
		StringBuilder result = new StringBuilder();
		for (Integer i : arr) {
			result.append((char) i.intValue());
		}
		return result.toString();
	}

	public static List<Integer> utf16StrToIntArr(String str) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < str.length(); i++) {
			result.add((int) str.charAt(i));
		}
		return result;
	}

	public static String compressToUtf16Str(String uncompressed) {
		List<Integer> arr = new LZWRosetta().compress(uncompressed);
		return intArrToUtf16Str(arr);
	}

	public static String decompressUtf16Str(String compressed) {
		List<Integer> arr = utf16StrToIntArr(compressed);
		return new LZWRosetta().decompress(arr);
	}

	static class LZWRosetta {
		/** Compress a string to a list of output symbols. */
		public List<Integer> compress(String uncompressed) {
			// Build the dictionary.
			int dictSize = 256;
			Map<String, Integer> dictionary = null;
			String w = "";
			List<Integer> result = new ArrayList<Integer>();
			for (char c : uncompressed.toCharArray()) {
				String wc = w + c;
				boolean reset = (dictSize == maxDictSize);
				if (dictionary == null || reset) {
					dictionary = new LinkedHashMap<String, Integer>();
					dictSize=256;
					for (int i = 0; i < 256; i++) {
						dictionary.put("" + (char) i, i);
					}
				}
				if (dictionary.containsKey(wc))
					w = wc;
				else {
					result.add(dictionary.get(w));
					// Add wc to the dictionary.
					dictionary.put(wc, dictSize++);
					w = "" + c;
				}
			}
			// Output the code for w.
			if (!w.equals(""))
				result.add(dictionary.get(w));
			return result;
		}

		/** Decompress a list of output ks to a string. */
		public String decompress(List<Integer> compressed) {
			// Build the dictionary.
			int dictSize = 256;
			Map<Integer, String> dictionary = null;
			String w = "" + (char) (int) compressed.remove(0);
			String result = w;
			for (int k : compressed) {
				String entry;
				boolean reset = (dictSize == maxDictSize);
				if (dictionary == null || reset) {
					dictSize=256;
					dictionary = new HashMap<Integer, String>();
					for (int i = 0; i < 256; i++) {
						dictionary.put(i, "" + (char) i);
					}
				}
				if (dictionary.containsKey(k))
					entry = dictionary.get(k);
				else if (k == dictSize)
					entry = w + w.charAt(0);
				else
					throw new IllegalArgumentException("Bad compressed k: " + k);
				result += entry;
				// Add w+entry[0] to the dictionary.
				dictionary.put(dictSize++, w + entry.charAt(0));
				w = entry;
			}
			return result;
		}
	}
}