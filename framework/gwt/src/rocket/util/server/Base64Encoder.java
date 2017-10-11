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
package rocket.util.server;

import rocket.util.client.Checker;

/**
 * This class may be used to base 64 encode a byte array into a String.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class Base64Encoder {
	/**
	 * Takes an array of bytes and builds a string that is base 64 encoded.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String encode(final byte[] bytes) {
		Checker.notNull("parameter:bytes", bytes);
		final StringBuffer buf = new StringBuffer();
		final int byteLength = bytes.length;
		int i = 0;
		int carriageReturnCountdown = 20;
		while (i < byteLength) {
			final int j = i + 1;
			final int k = i + 2;
			// encode the first byte...
			final byte a = bytes[i];
			final byte x = (byte) ((a >> 2) & 0x3f);
			buf.append(translateByte(x));
			final byte aa = (byte) ((a << 4) & 0x3f);
			// no more bytes write some padding and the last two bits of the $a
			if (j >= byteLength) {
				buf.append(translateByte(aa));
				buf.append('=');
				buf.append('=');
				break;
			}
			// encode the remainder of $a and half of $b
			final byte b = bytes[j];
			final byte y = (byte) (aa | ((b >> 4) & 0x0f));
			buf.append(translateByte(y));
			final byte bb = (byte) ((b << 2) & 0x3c);
			// check if there is a third byte...
			if (k >= byteLength) {
				buf.append(translateByte(bb));
				buf.append('=');
				break;
			}
			// write the third and fourth bytes...
			final byte c = bytes[k];
			final byte z = (byte) ((c >> 6) & 0x03);
			buf.append(translateByte((byte) (bb | z)));
			buf.append(translateByte((byte) (c & 0x3f)));
			// increment the index
			i = i + 3;
			carriageReturnCountdown--;
			if (carriageReturnCountdown == 0) {
				buf.append('\n');
				carriageReturnCountdown = 20;
			}
		}
		return buf.toString();
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	private static char translateByte(final byte in) {
		return (char) lookup[(in & 0xff)];
	}

	/**
	 * A lookup table containing the encoded value for a given set of 6 bits.
	 * This is used as an array of bytes for convenience and because the
	 * getBytes() method doesnt exist in GWT java.lang.String.
	 */
	private static char[] lookup = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
			.toCharArray();
	/*
	 * private static byte lookup[] = {(byte) 'A', (byte) 'B', (byte) 'C',
	 * (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', // 0-7 (byte)
	 * 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte)
	 * 'O', (byte) 'P', // 8-15 (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
	 * (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', // 16-23 (byte) 'Y',
	 * (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e',
	 * (byte) 'f', // 24-31 (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j',
	 * (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', // 32-39 (byte) 'o',
	 * (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
	 * (byte) 'v', // 40-47 (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
	 * (byte) '0', (byte) '1', (byte) '2', (byte) '3', // 48-55 (byte) '4',
	 * (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+',
	 * (byte) '/', // 56-63 (byte) '=' // 64
	 */
}