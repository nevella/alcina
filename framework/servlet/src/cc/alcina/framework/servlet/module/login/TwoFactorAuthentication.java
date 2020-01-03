package cc.alcina.framework.servlet.module.login;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

/*
 * https://www.javacodegeeks.com/2011/12/google-authenticator-using-it-with-your.html
 */
public class TwoFactorAuthentication {
	public String generateSecret() {
		// Allocating the buffer
		int scratchCodeSize = 12;
		int numOfScratchCodes = 0;
		int secretSize = 10;
		byte[] buffer = new byte[secretSize
				+ numOfScratchCodes * scratchCodeSize];
		// Filling the buffer with random numbers.
		// Notice: you want to reuse the same random generator
		// while generating larger random number sequences.
		new Random().nextBytes(buffer);
		Base32 codec = new Base32();
		byte[] secretKey = Arrays.copyOf(buffer, secretSize);
		byte[] bEncodedKey = codec.encode(secretKey);
		String encodedKey = new String(bEncodedKey);
		return encodedKey;
	}

	public String getQRBarcodeURL(String user, String host,
			String perUserSecret) {
		String format = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth:%s%s@%s%%3Fsecret%%3D%s";
		return String.format(format, "%2F%2Ftotp%2F", user, host,
				perUserSecret);
	}

	public boolean checkCode(String secret, long code, long t)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String codeOverride = ResourceUtilities.get("codeOverride");
		if (Ax.notBlank(codeOverride) && String.valueOf(code).equals(codeOverride)) {
			return true;
		}
		Base32 codec = new Base32();
		byte[] decodedKey = codec.decode(secret);
		// Window is used to check codes generated in the near past.
		// You can use this value to tune how far you're willing to go.
		int window = 3;
		for (int i = -window; i <= window; ++i) {
			long hash = verifyCode(decodedKey, t + i);
			if (hash == code) {
				return true;
			}
		}
		// The validation code is invalid.
		return false;
	}

	private int verifyCode(byte[] key, long t)
			throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] data = new byte[8];
		long value = t;
		for (int i = 8; i-- > 0; value >>>= 8) {
			data[i] = (byte) value;
		}
		SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);
		int offset = hash[20 - 1] & 0xF;
		// We're using a long because Java hasn't got unsigned int.
		long truncatedHash = 0;
		for (int i = 0; i < 4; ++i) {
			truncatedHash <<= 8;
			// We are dealing with signed bytes:
			// we just keep the first byte.
			truncatedHash |= (hash[offset + i] & 0xFF);
		}
		truncatedHash &= 0x7FFFFFFF;
		truncatedHash %= 1000000;
		return (int) truncatedHash;
	}
}
