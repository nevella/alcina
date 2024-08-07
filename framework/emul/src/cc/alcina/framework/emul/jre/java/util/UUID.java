/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 only, as published by
 * the Free Software Foundation. Oracle designates this particular file as
 * subject to the "Classpath" exception as provided by Oracle in the LICENSE
 * file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more
 * details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or
 * visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.util;


import java.util.Arrays;

public final class UUID implements java.io.Serializable, Comparable<UUID> {
	private static final byte[] NIBBLES;
	static {
		byte[] ns = new byte[256];
		Arrays.fill(ns, (byte) -1);
		ns['0'] = 0;
		ns['1'] = 1;
		ns['2'] = 2;
		ns['3'] = 3;
		ns['4'] = 4;
		ns['5'] = 5;
		ns['6'] = 6;
		ns['7'] = 7;
		ns['8'] = 8;
		ns['9'] = 9;
		ns['A'] = 10;
		ns['B'] = 11;
		ns['C'] = 12;
		ns['D'] = 13;
		ns['E'] = 14;
		ns['F'] = 15;
		ns['a'] = 10;
		ns['b'] = 11;
		ns['c'] = 12;
		ns['d'] = 13;
		ns['e'] = 14;
		ns['f'] = 15;
		NIBBLES = ns;
	}

	static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
			'z' };

	/**
	 * Creates a {@code UUID} from the string standard representation as
	 * described in the {@link #toString} method.
	 *
	 * @param name
	 *            A string that specifies a {@code UUID}
	 *
	 * @return A {@code UUID} with the specified value
	 *
	 * @throws IllegalArgumentException
	 *             If name does not conform to the string representation as
	 *             described in {@link #toString}
	 *
	 */
	public static UUID fromString(String name) {
		if (name.length() == 36) {
			char ch1 = name.charAt(8);
			char ch2 = name.charAt(13);
			char ch3 = name.charAt(18);
			char ch4 = name.charAt(23);
			if (ch1 == '-' && ch2 == '-' && ch3 == '-' && ch4 == '-') {
				long msb1 = parse4Nibbles(name, 0);
				long msb2 = parse4Nibbles(name, 4);
				long msb3 = parse4Nibbles(name, 9);
				long msb4 = parse4Nibbles(name, 14);
				long lsb1 = parse4Nibbles(name, 19);
				long lsb2 = parse4Nibbles(name, 24);
				long lsb3 = parse4Nibbles(name, 28);
				long lsb4 = parse4Nibbles(name, 32);
				if ((msb1 | msb2 | msb3 | msb4 | lsb1 | lsb2 | lsb3
						| lsb4) >= 0) {
					return new UUID(msb1 << 48 | msb2 << 32 | msb3 << 16 | msb4,
							lsb1 << 48 | lsb2 << 32 | lsb3 << 16 | lsb4);
				}
			}
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
	 *
	 * The {@code UUID} is generated using a cryptographically strong pseudo
	 * random number generator.
	 *
	 * @return A randomly generated {@code UUID}
	 */
	public static UUID randomUUID() {
		byte[] randomBytes = new byte[16];
		for (int idx = 0; idx < 16; idx++) {
			randomBytes[idx] = (byte) (Math.random() * 256);
		}
		randomBytes[6] &= 0x0f; /* clear version */
		randomBytes[6] |= 0x40; /* set to version 4 */
		randomBytes[8] &= 0x3f; /* clear variant */
		randomBytes[8] |= 0x80; /* set to IETF variant */
		return new UUID(randomBytes);
	}

	private static void formatUnsignedLong0(long val, int shift, byte[] buf,
			int offset, int len) {
		int charPos = offset + len;
		int radix = 1 << shift;
		int mask = radix - 1;
		do {
			buf[--charPos] = (byte) digits[((int) val) & mask];
			val >>>= shift;
		} while (charPos > offset);
	}
	// Field Accessor Methods

	private static long parse4Nibbles(String name, int pos) {
		byte[] ns = NIBBLES;
		char ch1 = name.charAt(pos);
		char ch2 = name.charAt(pos + 1);
		char ch3 = name.charAt(pos + 2);
		char ch4 = name.charAt(pos + 3);
		return (ch1 | ch2 | ch3 | ch4) > 0xff ? -1
				: ns[ch1] << 12 | ns[ch2] << 8 | ns[ch3] << 4 | ns[ch4];
	}

	static String fastUUID(long lsb, long msb) {
		byte[] buf = new byte[36];
		formatUnsignedLong0(lsb, 4, buf, 24, 12);
		formatUnsignedLong0(lsb >>> 48, 4, buf, 19, 4);
		formatUnsignedLong0(msb, 4, buf, 14, 4);
		formatUnsignedLong0(msb >>> 16, 4, buf, 9, 4);
		formatUnsignedLong0(msb >>> 32, 4, buf, 0, 8);
		buf[23] = '-';
		buf[18] = '-';
		buf[13] = '-';
		buf[8] = '-';
		return new String(buf);
	}

	/*
	 * The most significant 64 bits of this UUID.
	 *
	 * @serial
	 */
	private final long mostSigBits;

	/*
	 * The least significant 64 bits of this UUID.
	 *
	 * @serial
	 */
	private final long leastSigBits;

	/**
	 * Constructs a new {@code UUID} using the specified data. {@code
	 * mostSigBits} is used for the most significant 64 bits of the {@code
	 * UUID} and {@code leastSigBits} becomes the least significant 64 bits of
	 * the {@code UUID}.
	 *
	 * @param mostSigBits
	 *            The most significant bits of the {@code UUID}
	 *
	 * @param leastSigBits
	 *            The least significant bits of the {@code UUID}
	 */
	public UUID(long mostSigBits, long leastSigBits) {
		this.mostSigBits = mostSigBits;
		this.leastSigBits = leastSigBits;
	}

	private UUID(byte[] data) {
		long msb = 0;
		long lsb = 0;
		assert data.length == 16 : "data must be 16 bytes in length";
		for (int i = 0; i < 8; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		for (int i = 8; i < 16; i++)
			lsb = (lsb << 8) | (data[i] & 0xff);
		this.mostSigBits = msb;
		this.leastSigBits = lsb;
	}

	/**
	 * The clock sequence value associated with this UUID.
	 *
	 * <p>
	 * The 14 bit clock sequence value is constructed from the clock sequence
	 * field of this UUID. The clock sequence field is used to guarantee
	 * temporal uniqueness in a time-based UUID.
	 *
	 * <p>
	 * The {@code clockSequence} value is only meaningful in a time-based UUID,
	 * which has version type 1. If this UUID is not a time-based UUID then this
	 * method throws UnsupportedOperationException.
	 *
	 * @return The clock sequence of this {@code UUID}
	 *
	 * @throws UnsupportedOperationException
	 *             If this UUID is not a version 1 UUID
	 */
	public int clockSequence() {
		if (version() != 1) {
			throw new UnsupportedOperationException("Not a time-based UUID");
		}
		return (int) ((leastSigBits & 0x3FFF000000000000L) >>> 48);
	}

	/**
	 * Compares this UUID with the specified UUID.
	 *
	 * <p>
	 * The first of two UUIDs is greater than the second if the most significant
	 * field in which the UUIDs differ is greater for the first UUID.
	 *
	 * @param val
	 *            {@code UUID} to which this {@code UUID} is to be compared
	 *
	 * @return -1, 0 or 1 as this {@code UUID} is less than, equal to, or
	 *         greater than {@code val}
	 *
	 */
	@Override
	public int compareTo(UUID val) {
		// The ordering is intentionally set up so that the UUIDs
		// can simply be numerically compared as two numbers
		return (this.mostSigBits < val.mostSigBits ? -1
				: (this.mostSigBits > val.mostSigBits ? 1
						: (this.leastSigBits < val.leastSigBits ? -1
								: (this.leastSigBits > val.leastSigBits ? 1
										: 0))));
	}

	/**
	 * Compares this object to the specified object. The result is {@code
	 * true} if and only if the argument is not {@code null}, is a {@code UUID}
	 * object, has the same variant, and contains the same value, bit for bit,
	 * as this {@code UUID}.
	 *
	 * @param obj
	 *            The object to be compared
	 *
	 * @return {@code true} if the objects are the same; {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if ((null == obj) || (obj.getClass() != UUID.class))
			return false;
		UUID id = (UUID) obj;
		return (mostSigBits == id.mostSigBits
				&& leastSigBits == id.leastSigBits);
	}
	// Comparison Operations

	/**
	 * Returns the least significant 64 bits of this UUID's 128 bit value.
	 *
	 * @return The least significant 64 bits of this UUID's 128 bit value
	 */
	public long getLeastSignificantBits() {
		return leastSigBits;
	}

	/**
	 * Returns the most significant 64 bits of this UUID's 128 bit value.
	 *
	 * @return The most significant 64 bits of this UUID's 128 bit value
	 */
	public long getMostSignificantBits() {
		return mostSigBits;
	}

	/**
	 * Returns a hash code for this {@code UUID}.
	 *
	 * @return A hash code value for this {@code UUID}
	 */
	@Override
	public int hashCode() {
		long hilo = mostSigBits ^ leastSigBits;
		return ((int) (hilo >> 32)) ^ (int) hilo;
	}

	/**
	 * The node value associated with this UUID.
	 *
	 * <p>
	 * The 48 bit node value is constructed from the node field of this UUID.
	 * This field is intended to hold the IEEE 802 address of the machine that
	 * generated this UUID to guarantee spatial uniqueness.
	 *
	 * <p>
	 * The node value is only meaningful in a time-based UUID, which has version
	 * type 1. If this UUID is not a time-based UUID then this method throws
	 * UnsupportedOperationException.
	 *
	 * @return The node value of this {@code UUID}
	 *
	 * @throws UnsupportedOperationException
	 *             If this UUID is not a version 1 UUID
	 */
	public long node() {
		if (version() != 1) {
			throw new UnsupportedOperationException("Not a time-based UUID");
		}
		return leastSigBits & 0x0000FFFFFFFFFFFFL;
	}
	// Object Inherited Methods

	/**
	 * The timestamp value associated with this UUID.
	 *
	 * <p>
	 * The 60 bit timestamp value is constructed from the time_low, time_mid,
	 * and time_hi fields of this {@code UUID}. The resulting timestamp is
	 * measured in 100-nanosecond units since midnight, October 15, 1582 UTC.
	 *
	 * <p>
	 * The timestamp value is only meaningful in a time-based UUID, which has
	 * version type 1. If this {@code UUID} is not a time-based UUID then this
	 * method throws UnsupportedOperationException.
	 *
	 * @throws UnsupportedOperationException
	 *             If this UUID is not a version 1 UUID
	 * @return The timestamp of this {@code UUID}.
	 */
	public long timestamp() {
		if (version() != 1) {
			throw new UnsupportedOperationException("Not a time-based UUID");
		}
		return (mostSigBits & 0x0FFFL) << 48
				| ((mostSigBits >> 16) & 0x0FFFFL) << 32 | mostSigBits >>> 32;
	}

	/**
	 * Returns a {@code String} object representing this {@code UUID}.
	 *
	 * <p>
	 * The UUID string representation is as described by this BNF: <blockquote>
	 *
	 * <pre>
	 * {@code
	 * UUID                   = <time_low> "-" <time_mid> "-"
	 *                          <time_high_and_version> "-"
	 *                          <variant_and_sequence> "-"
	 *                          <node>
	 * time_low               = 4*<hexOctet>
	 * time_mid               = 2*<hexOctet>
	 * time_high_and_version  = 2*<hexOctet>
	 * variant_and_sequence   = 2*<hexOctet>
	 * node                   = 6*<hexOctet>
	 * hexOctet               = <hexDigit><hexDigit>
	 * hexDigit               =
	 *       "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
	 *       | "a" | "b" | "c" | "d" | "e" | "f"
	 *       | "A" | "B" | "C" | "D" | "E" | "F"
	 * }</pre>
	 *
	 * </blockquote>
	 *
	 * @return A string representation of this {@code UUID}
	 */
	@Override
	public String toString() {
		return fastUUID(leastSigBits, mostSigBits);
	}

	/**
	 * The variant number associated with this {@code UUID}. The variant number
	 * describes the layout of the {@code UUID}.
	 *
	 * The variant number has the following meaning:
	 * <ul>
	 * <li>0 Reserved for NCS backward compatibility
	 * <li>2
	 * <a href="http://www.ietf.org/rfc/rfc4122.txt">IETF&nbsp;RFC&nbsp;4122</a>
	 * (Leach-Salz), used by this class
	 * <li>6 Reserved, Microsoft Corporation backward compatibility
	 * <li>7 Reserved for future definition
	 * </ul>
	 *
	 * @return The variant number of this {@code UUID}
	 */
	public int variant() {
		// This field is composed of a varying number of bits.
		// 0 - - Reserved for NCS backward compatibility
		// 1 0 - The IETF aka Leach-Salz variant (used by this class)
		// 1 1 0 Reserved, Microsoft backward compatibility
		// 1 1 1 Reserved for future definition.
		return (int) ((leastSigBits >>> (64 - (leastSigBits >>> 62)))
				& (leastSigBits >> 63));
	}

	/**
	 * The version number associated with this {@code UUID}. The version number
	 * describes how this {@code UUID} was generated.
	 *
	 * The version number has the following meaning:
	 * <ul>
	 * <li>1 Time-based UUID
	 * <li>2 DCE security UUID
	 * <li>3 Name-based UUID
	 * <li>4 Randomly generated UUID
	 * </ul>
	 *
	 * @return The version number of this {@code UUID}
	 */
	public int version() {
		// Version is bits masked by 0x000000000000F000 in MS long
		return (int) ((mostSigBits >> 12) & 0x0f);
	}
}
