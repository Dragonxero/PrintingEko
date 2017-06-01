package com.incomrecycle.common.utils;

import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

public class EncryptUtils {
	public static byte[] DESEncryptVi(byte[] src, byte[] key, byte[] iv)
			throws Exception {
		return DESVi("DES/CBC/PKCS5Padding", src, formatDESKey(key,8), formatDESKey(iv,8),
				Cipher.ENCRYPT_MODE);
	}

	public static byte[] DESDecryptVi(byte[] encsrc, byte[] key, byte[] iv)
			throws Exception {
		return DESVi("DES/CBC/PKCS5Padding", encsrc, formatDESKey(key,8), formatDESKey(iv,8),
				Cipher.DECRYPT_MODE);
	}

	public static byte[] DESEncrypt(byte[] src, byte[] key) throws Exception {
		return DES("DES", src, formatDESKey(key, 8), Cipher.ENCRYPT_MODE);
	}

	public static byte[] DESDecrypt(byte[] encsrc, byte[] key) throws Exception {
		return DES("DES", encsrc, formatDESKey(key, 8),
				Cipher.DECRYPT_MODE);
	}

	public static byte[] DES3Encrypt(byte[] src, byte[] key) throws Exception {
		return DES("DESede", src, formatDESKey(key, 24),
				Cipher.ENCRYPT_MODE);
	}

	public static byte[] DES3Decrypt(byte[] encsrc, byte[] key)
			throws Exception {
		return DES("DESede", encsrc, formatDESKey(key, 24),
				Cipher.DECRYPT_MODE);
	}

	public static byte[] DESVi(String algorithm, byte[] encsrc,
			byte[] key, byte[] iv, int MODE) throws Exception {
		DESKeySpec keySpec = new DESKeySpec(key);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key k = keyFactory.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(MODE, k, ivParameterSpec);
		return cipher.doFinal(encsrc);
	}
	public static byte[] RC4(byte[] src, byte[] key) {

		int[] iS = new int[256];
		byte[] iK = new byte[256];

		for (int i = 0; i < 256; i++)
			iS[i] = i;

		for (short i = 0; i < 256; i++) {
			iK[i] = key[i % key.length];
		}
		int j = 0;

		for (int i = 0; i < 256; i++) {
			j = (j + iS[i] + iK[i]) % 256;
			int temp = iS[i];
			iS[i] = iS[j];
			iS[j] = temp;
		}

		int i = 0;
		j = 0;
		byte[] iOutput = new byte[src.length];
		for (short x = 0; x < src.length; x++) {
			i = (i + 1) % 256;
			j = (j + iS[i]) % 256;
			int temp = iS[i];
			iS[i] = iS[j];
			iS[j] = temp;
			int t = (iS[i] + (iS[j] % 256)) % 256;
			iOutput[x] = (byte) (src[x] ^ iS[t]);
		}

		return iOutput;
	}

	private static byte[] formatDESKey(byte[] key, int length) {
		byte[] newkey = null;
		if (key.length == length)
			newkey = key;
		else {
			newkey = new byte[length];
			for (int i = 0; i < length; i++)
				newkey[i] = 0;
			for (int i = 0; i < key.length; i++) {
				if (i >= length)
					break;
				newkey[i] = key[i];
			}
		}
		return newkey;
	}

	private static byte[] DES(String algorithm, byte[] src, byte[] key,
			int MODE) throws Exception {
		SecretKey securekey = new SecretKeySpec(key, algorithm);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(MODE, securekey);
		return cipher.doFinal(src);
	}

	public static byte[] encryptMD5(byte[] src, byte[] key) {
		int b = 0;
		int idx = 0;
		byte[] mod = md5(key);
		byte[] seed = new byte[key.length + mod.length];
		System.arraycopy(mod, 0, seed, 0, mod.length);
		System.arraycopy(key, 0, seed, mod.length, key.length);
		mod = md5(seed);
		byte[] res = new byte[src.length];
		while (idx < src.length) {
			res[idx] = (byte) (src[idx] + mod[b]);
			idx++;
			b++;
			if (b == mod.length && idx < src.length) {
				System.arraycopy(src, idx - mod.length, seed, 0, mod.length);
				mod = md5(seed);
				b = 0;
			}
		}
		return res;
	}

	public static byte[] decryptMD5(byte[] src, byte[] key) {
		int b = 0;
		int idx = 0;
		byte[] mod = md5(key);
		byte[] seed = new byte[key.length + mod.length];
		System.arraycopy(mod, 0, seed, 0, mod.length);
		System.arraycopy(key, 0, seed, mod.length, key.length);
		mod = md5(seed);
		byte[] res = new byte[src.length];
		while (idx < src.length) {
			res[idx] = (byte) (src[idx] - mod[b]);
			idx++;
			b++;
			if (b == mod.length && idx < src.length) {
				System.arraycopy(res, idx - mod.length, seed, 0, mod.length);
				mod = md5(seed);
				b = 0;
			}
		}
		return res;
	}

	public static final String md5(String[] strSrcs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strSrcs.length; i++) {
			if (strSrcs[i] != null)
				sb.append(strSrcs[i]);
		}
		return md5(sb.toString());
	}

	public static final byte[] messageDigest(String algorithm, byte[] src) {
		try {
			MessageDigest alg = MessageDigest.getInstance(algorithm);
			return alg.digest(src);
		} catch (Exception e) {
			return null;
		}
	}

	public static final byte[] md5(byte[] src) {
		return messageDigest("MD5", src);
	}

	public static final String md5(String src) {
		return byte2hex(messageDigest("MD5", src.getBytes()));
	}

	public static final String sha1(String src) {
		return byte2hex(messageDigest("SHA-1", src.getBytes()));
	}

	public static String byte_base64(byte[] data) {
		if (data == null)
			return null;
		return Base64.encodeToString(data, Base64.DEFAULT);
	}

	public static byte[] base64_byte(String str) {
		if (str == null)
			return null;
		try {
			return Base64.decode(str, Base64.DEFAULT);
		} catch (Exception e) {
			return null;
		}
	}

	public static String byte2hex(byte[] data) {
		return byte2hex(data, 0, data.length);
	}

	public static String byte2hex(byte[] data, int offset, int length) {
		if (data == null)
			return null;
		if (data.length <= offset)
			return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			String strByte = Integer.toHexString(data[i + offset]);
			if (strByte.length() < 2)
				strByte = "0" + strByte;
			sb.append(strByte.substring(strByte.length() - 2));
		}
		return sb.toString();
	}

	public static byte[] hex2byte(String str) {
		if (str == null)
			return null;
		if (str.length() == 0)
			return new byte[] {};
		int len = str.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2),
					16);
		}
		return data;
	}

	public static void htonl(int v, byte[] buff, int offset) {
		buff[offset + 0] = (byte) ((v >> 24) & 0xFF);
		buff[offset + 1] = (byte) ((v >> 16) & 0xFF);
		buff[offset + 2] = (byte) ((v >> 8) & 0xFF);
		buff[offset + 3] = (byte) ((v) & 0xFF);
	}

	public static int ntohl(byte[] buff, int offset) {
		int v = 0;
		v += (((int) (buff[offset + 0])) << 24) & 0xFF000000;
		v += (((int) (buff[offset + 1])) << 16) & 0x00FF0000;
		v += (((int) (buff[offset + 2])) << 8) & 0x0000FF00;
		v += (((int) (buff[offset + 3]))) & 0x000000FF;
		return v;
	}

	public static void htons(int v, byte[] buff, int offset) {
		buff[offset + 0] = (byte) ((v >> 8) & 0xFF);
		buff[offset + 1] = (byte) ((v) & 0xFF);
	}

	public static int ntohs(byte[] buff, int offset) {
		int v = 0;
		v += (((int) (buff[offset + 0])) << 8) & 0x0000FF00;
		v += (((int) (buff[offset + 1]))) & 0x000000FF;
		return v;
	}
}
