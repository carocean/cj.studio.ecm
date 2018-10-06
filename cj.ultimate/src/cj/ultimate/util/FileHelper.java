package cj.ultimate.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FileHelper {
	/**
	 * 只扫描两层目录
	 * <pre>
	 *
	 * </pre>
	 * @param pluginDir
	 * @param jars
	 */
	public static void scansJarFiles(File pluginDir, List<File> jars) {
		FileFilter filter=new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				boolean isJar = f.getName().endsWith(".jar");
				if (isJar) {
					jars.add(f);
				}
				return false;
			}
		};
		File[] dirs=pluginDir.listFiles(filter);
		for(File dir:dirs){
			dir.listFiles(filter);
		}
	}
	public static void scansSubAllJarFiles(File dir, List<File> jars) {
		FileFilter filter=new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				boolean isJar = f.getName().endsWith(".jar");
				if (isJar) {
					jars.add(f);
				}
				return false;
			}
		};
		File[] dirs=dir.listFiles(filter);
		if(dirs==null)return;
		for(File d:dirs){
			scansSubAllJarFiles(d, jars);
		}
	}
	/**
     * Deletes all files and subdirectories under "dir".
     * @param dir Directory to be deleted
     * @return boolean Returns "true" if all deletions were successful.
     *                 If a deletion fails, the method stops attempting to
     *                 delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
 
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
 
        // The directory is now empty so now it can be smoked
        return dir.delete();
    }
    public static String getFilePathNoEx(String file) {
		if ((file != null) && (file.length() > 0)) {
			file=file.replace("\\", "/").replace("//", "/");
			int dot = file.lastIndexOf('.');
			if ((dot > -1) && (dot < (file.length()))) {
				return file.substring(0, dot);
			}
		}
		return file;
	}
	public static String getFileNameNoEx(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			filename=filename.replace("\\", "/");
			filename=filename.substring(filename.lastIndexOf("/")+1,filename.length());
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length()))) {
				return filename.substring(0, dot);
			}
		}
		return filename;
	}
	public static void  main(String...strings){
		System.out.println(getFileNameNoEx("/xxx\\xx.tt"));
	}
	//本类注释掉的原因：当文件超过一定大小时，文件不被完全读取，原因是：in.available()只是每次可读数，并不是整个文件长。
	//因此该问题曾导致：织入类出错，现将systemresource装载织入类改为另一种方法实现
//	public static byte[] readFully(InputStream in) throws IOException {
//		DataInputStream dis = null;
//		try {
//			dis = new DataInputStream(in);
//			byte[] b = new byte[(int) in.available()];
//			dis.readFully(b);
//			return b;
//		} catch (IOException e) {
//			throw new IOException(e);
//		} finally {
//			if (dis != null)
//				dis.close();
//		}
//	}
	public static byte[] readFully(InputStream in) throws IOException {
		int blen=2048;
		ByteArrayOutputStream buf=new ByteArrayOutputStream(blen*2);
		int timeRead=0;
		byte[] b=new byte[blen];
		try{
		while((timeRead=in.read(b, 0,blen ))>0){
			buf.write(b, 0, timeRead);
		}
		return buf.toByteArray();
		}catch(IOException e){
			throw new IOException(e);
		}finally {
			if (in != null)
				in.close();
		}
	}
	public static byte[] readFully(File f) throws IOException {
		FileInputStream in =null;
		try {
			if (f.length() >= Integer.MAX_VALUE) {
				throw new IOException(String.format("要读取的文档太大，不能超过%s个字节",
						Integer.MAX_VALUE));
			}
			in = new FileInputStream(f);
			return readFully(in);
		} catch (IOException e) {
			throw e;
		} finally {
			if (in != null)
				in.close();
		}
	}
	public static void copy(File src, String toDir) {
		InputStream inStream = null;
		FileOutputStream fs = null;
		try {
			int byteread = 0;
			inStream = new FileInputStream(src); // 读入原文件
			File f = new File(toDir);
			if (!f.exists()) {
				f.mkdirs();
			}
			fs = new FileOutputStream(toDir + File.separator + src.getName());
			byte[] buffer = new byte[1444];
			while ((byteread = inStream.read(buffer)) != -1) {
				fs.write(buffer, 0, byteread);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);

		} finally {
			try {
				if (inStream != null)
					inStream.close();
				if (fs != null)
					fs.close();
			} catch (IOException e) {

			}
		}
	}
}
