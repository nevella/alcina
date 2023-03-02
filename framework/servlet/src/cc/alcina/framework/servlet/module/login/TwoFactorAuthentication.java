package cc.alcina.framework.servlet.module.login;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;

/*
 * https://www.javacodegeeks.com/2011/12/google-authenticator-using-it-with-your
 * .html
 */
public class TwoFactorAuthentication {
	public boolean checkCode(String secret, long code, long t)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String codeOverride = Configuration.get("codeOverride");
		if (Ax.notBlank(codeOverride)
				&& String.valueOf(code).equals(codeOverride)) {
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

	/**
	 * Generate a TOTP code to authenticate with 2FA on a remote application
	 * 
	 * @param secret
	 *            2FA secret
	 * @return TOTP code
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public long generateTOTPCode(String secret)
			throws InvalidKeyException, NoSuchAlgorithmException {
		// Get `t` value for the current point in time
		long t = new Date().getTime() / TimeUnit.SECONDS.toMillis(30);
		// Decode secret string from a base 32 string
		Base32 codec = new Base32();
		byte[] decodedKey = codec.decode(secret);
		// Generate the TOTP code
		return verifyCode(decodedKey, t);
	}

	public String getQRBarcodeURL(String user, String host,
			String perUserSecret) {
		String format = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&"
				+ "cht=qr&chl=otpauth:%s%s%%3Fsecret%%3D%s%%26issuer%%3D%s";
		return String.format(format, "%2F%2Ftotp%2F", user, perUserSecret,
				host);
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
