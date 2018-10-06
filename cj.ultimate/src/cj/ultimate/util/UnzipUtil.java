package cj.ultimate.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class UnzipUtil {
	public static void unzip(String className, String searchFolder,
			String outFolder, String inzipEntry) throws IOException {
		FindInJar findInJar = new FindInJar(className);

		@SuppressWarnings("unchecked")
		List<String> jarFiles = findInJar.findClass(searchFolder, true);
		byte[] bytes = new byte[1024];
		if (!jarFiles.isEmpty()) {
			JarFile jarfile = new JarFile(jarFiles.get(0));
			Enumeration<JarEntry> e = jarfile.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				if (entry == null)
					break;
				if (entry.getName().startsWith(inzipEntry)) {
					File desTemp = new File(outFolder + File.separator
							+ entry.getName().replace(inzipEntry, ""));

					if (entry.isDirectory()) { // jar条目是空目录
						if (!desTemp.exists())
							desTemp.mkdirs();
					} else { // jar条目是文件
						BufferedOutputStream out = new BufferedOutputStream(
								new FileOutputStream(desTemp));
						InputStream jis = jarfile.getInputStream(entry);
						int len = jis.read(bytes, 0, bytes.length);
						while (len != -1) {
							out.write(bytes, 0, len);
							len = jis.read(bytes, 0, bytes.length);
						}

						out.flush();
						out.close();
						jis.close();
					}

				}
			}

			jarfile.close();
		}

	}
	public static void unzip(String file, String inzipEntry, String outFolder)
			throws IOException {
		File f=new File(outFolder);
		if(!f.exists()){
			f.mkdirs();
		}
		if(inzipEntry.contains(".")){
			inzipEntry=inzipEntry.replace(".", "/");
		}
		if(inzipEntry.startsWith("/")){
			inzipEntry=inzipEntry.substring(1,inzipEntry.length());
		}
		JarFile jarfile = new JarFile(file);
		Enumeration<JarEntry> e = jarfile.entries();

		while (e.hasMoreElements()) {
			JarEntry entry = e.nextElement();
			if (entry == null)
				break;
			if (entry.getName().startsWith(inzipEntry)) {
				File desTemp = new File(outFolder + File.separator
						+ entry.getName().replace(inzipEntry, ""));

				if (entry.isDirectory()) { // jar条目是空目录
					if (!desTemp.exists())
						desTemp.mkdirs();
				} else { // jar条目是文件
					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream(desTemp));
					InputStream jis = jarfile.getInputStream(entry);
					byte[] bytes = new byte[1024];
					int len = jis.read(bytes, 0, bytes.length);
					while (len != -1) {
						out.write(bytes, 0, len);
						len = jis.read(bytes, 0, bytes.length);
					}

					out.flush();
					out.close();
					jis.close();
				}

			}
		}
		jarfile.close();
	}
}
