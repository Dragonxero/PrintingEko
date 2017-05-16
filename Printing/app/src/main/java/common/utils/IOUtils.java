package common.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
	public static InputStream close(InputStream is) {
		try {
			if(is != null)
				is.close();
		} catch(Exception e) {
		}
		return null;
	}
	public static OutputStream close(OutputStream os) {
		try {
			if(os != null)
				os.close();
		} catch(Exception e) {
		}
		return null;
	}
	public static byte[] read(InputStream is) {
		byte[] buff = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int readLen;
		try {
			while((readLen = is.read(buff)) > 0) {
				baos.write(buff, 0, readLen);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	public static long dump(InputStream is, OutputStream os) {
		byte[] buff = new byte[1024];
		int readLen;
		long lLen = 0;
		try {
			while((readLen = is.read(buff)) > 0) {
				os.write(buff, 0, readLen);
				lLen += readLen;
			}
			return lLen;
		} catch (IOException e) {
			return 0;
		}
	}
	public static byte[] readFile(String filename) {
		File file = new File(filename);
		if(!file.isFile())
			return null;
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			if(is == null)
				return null;
			return read(is);
		} catch(Exception e) {
			return null;
		} finally {
			close(is);
		}
	}
	public static InputStream getResourceAsInputStream(String resource) {
		return IOUtils.class.getClassLoader().getResourceAsStream(resource);
	}
	public static byte[] readResource(String resource) {
		InputStream is = null;
		try {
			is = IOUtils.class.getClassLoader().getResourceAsStream(resource);
			if(is == null)
				return null;
			return read(is);
		} catch(Exception e) {
			return null;
		} finally {
			close(is);
		}
	}
}
