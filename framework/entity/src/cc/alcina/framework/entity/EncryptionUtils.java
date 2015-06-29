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
package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*The "large" functions assume a byte structure of [first 128] public-key RSA encrypted 3DES key, byte 129 onwards 3dse encrypted data
 * 
 */
/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class EncryptionUtils {
	public static void showProviders() {
		try {
			Provider[] providers = Security.getProviders();
			for (int i = 0; i < providers.length; i++) {
				System.out.println("Provider: " + providers[i].getName() + ", "
						+ providers[i].getInfo());
				for (Iterator itr = providers[i].keySet().iterator(); itr
						.hasNext();) {
					String key = (String) itr.next();
					String value = (String) providers[i].get(key);
					System.out.println("\t" + key + " = " + value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private byte[] passphrase;

	private byte[] publicKeyBytes;

	private byte[] privateKeyBytes;

	private PrivateKey privateKey;

	private PublicKey publicKey;

	/**
	 * Don't use for SHA1
	 * 
	 */
	public static EncryptionUtils get() {
		EncryptionUtils singleton = Registry
				.checkSingleton(EncryptionUtils.class);
		if (singleton == null) {
			singleton = new EncryptionUtils();
			Registry.registerSingleton(EncryptionUtils.class, singleton);
		}
		return singleton;
	}

	// will decrypt then gunzip
	public byte[] asymDecryptLarge(byte[] source) {
		try {
			byte[] publicEncryptedSymKey = new byte[128];
			System.arraycopy(source, 0, publicEncryptedSymKey, 0, 128);
			byte[] desKeyDecoded = asymmetricDecrypt(publicEncryptedSymKey,
					this.privateKey);
			byte[] encData = new byte[source.length - 128];
			DESedeKeySpec pass = new DESedeKeySpec(desKeyDecoded);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
			SecretKey s = skf.generateSecret(pass);
			System.arraycopy(source, 128, encData, 0, source.length - 128);
			byte[] bs = symmetricDecrypt(encData, s);
			return gunzipBytes(bs);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// will gzip then encrypt
	public byte[] asymEncryptLarge(byte[] source) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			KeyGenerator keygen = KeyGenerator.getInstance("DESede");
			SecretKey desKey = keygen.generateKey();
			byte[] publicEncryptedSymKey = asymmetricEncrypt(
					desKey.getEncoded(), this.publicKey);
			baos.write(publicEncryptedSymKey);
			byte[] gzs = gzipBytes(source);
			baos.write(symmetricEncrypt(gzs, desKey));
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public byte[] asymmetricDecrypt(byte[] source, Key key) {
		try {
			Cipher desCipher = Cipher.getInstance("RSA");
			desCipher.init(Cipher.DECRYPT_MODE, key);
			return desCipher.doFinal(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public byte[] asymmetricEncrypt(byte[] source, Key key) {
		try {
			Cipher desCipher = Cipher.getInstance("RSA");
			desCipher.init(Cipher.ENCRYPT_MODE, key);
			return desCipher.doFinal(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public boolean checkPassphrase() {
		try {
			keyPairFromBytes();
		} catch (Exception e) {
			return false;
		}
		String msg = "hi world";
		byte[] src = msg.getBytes();
		byte[] res = this.asymmetricEncrypt(src, this.getPublicKey());
		byte[] res2 = this.asymmetricDecrypt(res, this.getPrivateKey());
		return (new String(res2).equals(msg));
	}

	public void generateKeys() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			KeyPair pair = keyGen.generateKeyPair();
			byte[] unencPrivate = pair.getPrivate().getEncoded();
			Key key = getSymmetricKey(getPassphrase());
			this.privateKeyBytes = symmetricEncrypt(unencPrivate, key);
			this.publicKeyBytes = pair.getPublic().getEncoded();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	/**
	 * @return the passphrase
	 */
	public byte[] getPassphrase() {
		return this.passphrase;
	}

	/**
	 * @return the privateKey
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * @return the privateKey
	 */
	public byte[] getPrivateKeyBytes() {
		return this.privateKeyBytes;
	}

	/**
	 * @return the publicKey
	 */
	public PublicKey getPublicKey() {
		return this.publicKey;
	}

	/**
	 * @return the publicKey
	 */
	public byte[] getPublicKeyBytes() {
		return this.publicKeyBytes;
	}

	public void keyPairFromBytes() throws Exception {
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		publicKey = keyFactory.generatePublic(pubKeySpec);
		if (getPassphrase() != null) {
			Key key = getSymmetricKey(getPassphrase());
			byte[] unencPrivate = symmetricDecrypt(getPrivateKeyBytes(), key);
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
					unencPrivate);
			privateKey = keyFactory.generatePrivate(privateKeySpec);
		}
	}

	/**
	 * @param passphrase
	 *            the passphrase to set
	 */
	public void setPassphrase(byte[] passphrase) {
		this.passphrase = passphrase;
	}

	/**
	 * @param privateKey
	 *            the privateKey to set
	 */
	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * @param privateKey
	 *            the privateKey to set
	 */
	public void setPrivateKeyBytes(byte[] privateKey) {
		this.privateKeyBytes = privateKey;
	}

	/**
	 * @param publicKey
	 *            the publicKey to set
	 */
	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * @param publicKey
	 *            the publicKey to set
	 */
	public void setPublicKeyBytes(byte[] publicKey) {
		this.publicKeyBytes = publicKey;
	}

	public byte[] symmetricDecrypt(byte[] source, Key key) {
		try {
			Cipher desCipher = Cipher.getInstance("DESede");
			desCipher.init(Cipher.DECRYPT_MODE, key);
			return desCipher.doFinal(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public byte[] symmetricEncrypt(byte[] source, Key key) {
		try {
			Cipher desCipher = Cipher.getInstance("DESede");
			desCipher.init(Cipher.ENCRYPT_MODE, key);
			return desCipher.doFinal(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private Key getSymmetricKey(byte[] encodedInfo) {
		try {
			int keylen = DESedeKeySpec.DES_EDE_KEY_LEN;
			if (encodedInfo.length < keylen) {
				byte[] newKey = new byte[keylen];
				Arrays.fill(newKey, (byte) 0);
				System.arraycopy(encodedInfo, 0, newKey, 0,
						getPassphrase().length);
				encodedInfo = newKey;
			} else {
				byte[] newKey = new byte[keylen];
				Arrays.fill(newKey, (byte) 0);
				System.arraycopy(encodedInfo, 0, newKey, 0, keylen);
				encodedInfo = newKey;
			}
			DESedeKeySpec pass = new DESedeKeySpec(encodedInfo);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
			SecretKey s = skf.generateSecret(pass);
			return s;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	byte[] gunzipBytes(byte[] bytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		ResourceUtilities.writeStreamToStream(gzis, baos);
		return baos.toByteArray();
	}

	byte[] gzipBytes(byte[] bytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		gzos.write(bytes, 0, bytes.length);
		gzos.flush();
		gzos.close();
		return baos.toByteArray();
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String MD5(String text) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("MD5");
		byte[] md5hash = new byte[32];
		// iso-8859 not that good an idea, but keep it...
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		md5hash = md.digest();
		return convertToHex(md5hash);
	}

	private MessageDigest sha1Digest;

	public String SHA1(String text) {
		try {
			MessageDigest md = ensureSha1Digest();
			byte[] sha1hash = new byte[32];
			md.update(text.getBytes("utf-8"), 0, text.length());
			sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private MessageDigest ensureSha1Digest() throws Exception {
		if (sha1Digest == null) {
			sha1Digest = MessageDigest.getInstance("SHA1");
		}
		return sha1Digest;
	}

	public static String MD5(byte[] bytes) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("MD5");
		byte[] md5hash = new byte[32];
		md.update(bytes, 0, bytes.length);
		md5hash = md.digest();
		return convertToHex(md5hash);
	}

	public static String MD5(List<byte[]> byties)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("MD5");
		for (byte[] bytes : byties) {
			md.update(bytes, 0, bytes.length);
		}
		byte[] md5hash = new byte[32];
		md5hash = md.digest();
		return convertToHex(md5hash);
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	public static final byte[] longToByteArray(long value) {
		return new byte[] { (byte) (value >>> 56), (byte) (value >>> 48),
				(byte) (value >>> 40), (byte) (value >>> 32),
				(byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}
}