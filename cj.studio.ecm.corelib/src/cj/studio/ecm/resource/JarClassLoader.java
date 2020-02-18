package cj.studio.ecm.resource;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.util.ObjectHelper;
import cj.ultimate.IDisposable;

/**
 * @uml.dependency supplier="cj.nns.ultimate.IDisposable"
 */
public class JarClassLoader extends URLClassLoader implements IDisposable {
    /**
     * VM parameter key to turn on debug logging to file or console.
     */
    public static final String KEY_LOGGER = "JarClassLoader.logger";
    public static final String CONSOLE = "console";

    private PrintStream logger;
    private final List<JarFile> lstJarFile;
    private final List<JarFile> overridesJarFile;
    private Set<File> hsNativeFile;
    private Map<String, Class<?>> hmClass;
    private ProtectionDomain pd;
    final private String referencePath = IResource.REFRECENCE_LIBPATH_FILE;
    final private String refembedPath = IResource.REFEMBED_LIBPATH_FILE;
    final private String refoverridePath = IResource.REFOVERRIDE_LIBPATH_FILE;

    /**
     * Default constructor. Defines system class loader as a parent class
     * loader.
     */
    public JarClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }

    public List<JarFile> getLstJarFile() {
        return lstJarFile;
    }

    /**
     * Constructor.
     *
     * @param parent class loader parent.
     */
    public JarClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        String sLogger = System.getProperty(KEY_LOGGER);
        if (sLogger != null) {
            if (sLogger.equals(CONSOLE)) {
                this.logger = System.out;
            } else {
                try {
                    this.logger = new PrintStream(sLogger);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(
                            "JarClassLoader: cannot create log file: " + e);
                }
            }
        }
        hmClass = new HashMap<String, Class<?>>();
        lstJarFile = new ArrayList<JarFile>();
        overridesJarFile = new ArrayList<JarFile>();
        hsNativeFile = new HashSet<File>();

        String sUrlTopJAR = null;
        //受保护域是主代码jar，因为它在构造所以每新建一个加载器都会被装载一次，在多层级的加载器结构中，它由于会被不同层级的加载器装载，因此应去掉它
//        try {
        pd = getClass().getProtectionDomain();
//            CodeSource cs = pd.getCodeSource();
//            URL urlTopJAR = cs.getLocation();
//            // URL.getFile() returns "/C:/my%20dir/MyApp.jar"
//            sUrlTopJAR = URLDecoder.decode(urlTopJAR.getFile(), "UTF-8");
//            log("Loading from top JAR: %s", sUrlTopJAR);
//            if (sUrlTopJAR.endsWith(".jar")) {
//                JarFile jarFile = new JarFile(sUrlTopJAR);
//                loadJar(jarFile, this.lstJarFile); // throws if not JAR
//                System.out.println("~~1~~~~~~~~~~~~" + jarFile.getName());
//            }
//        } catch (IOException e) {
//            // Expected exception: loading NOT from JAR.
//            log("Not a JAR: %s %s", sUrlTopJAR, e.toString());
//            return;
//        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    } // JarClassLoader()

    // --------------------------------separator--------------------------------
    static int ______INIT;

    /**
     * Using temp files (one per inner JAR/DLL) solves many issues: 1. There are
     * no ways to load JAR defined in a JarEntry directly into the JarFile
     * object. 2. Cannot use memory-mapped files because they are using nio
     * channels, which are not supported by JarFile ctor. 3. JarFile object
     * keeps opened JAR files handlers for fast access. 4. Resource in a
     * jar-in-jar does not have well defined URL. Making temp file with JAR
     * solves this problem. 5. Similar issues with native libraries:
     * <code>ClassLoader.findLibrary()</code> accepts ONLY string with absolute
     * path to the file with native library. 6. Option
     * "java.protocol.handler.pkgs" does not allow access to nested JARs(?).
     *
     * @param inf JAR entry information
     * @return temporary file object presenting JAR entry
     * @throws JarClassLoaderException
     */
    private File createTempFile(JarEntryInfo inf)
            throws JarClassLoaderException {
        byte[] a_by = inf.getJarBytes();
        try {
            File file = File.createTempFile(inf.getName() + ".", null);
            file.deleteOnExit();
            BufferedOutputStream os = new BufferedOutputStream(
                    new FileOutputStream(file));
            os.write(a_by);
            os.close();
            return file;
        } catch (IOException e) {
            throw new JarClassLoaderException(
                    "Cannot create temp file for " + inf.jarEntry, e);
        }
    } // createTempFile()

    protected void unLoadJar() {
        this.lstJarFile.clear();
        this.hsNativeFile.clear();
        this.hmClass.clear();
        this.hsNativeFile = null;
        this.hmClass = null;
    }

    public void loadJar(String file) {
        JarFile f;
        try {
            f = new JarFile(file);
            //将f添加到this.lstJarFile
            this.loadJar(f, this.lstJarFile);
//            System.out.println("~~2~~~~~~~~~~~~"+f.getName());
        } catch (IOException e) {
            log("Loading inner JAR %s from temp file %s", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * �ڼ���jar��ÿһ���Ŀ¼ʱ����
     *
     * @param entry
     */
    protected void onLoadJar(JarEntry entry) {

    }

    /**
     * Loads specified JAR
     *
     * @param jarFile JAR file
     */
    protected void loadJar(JarFile jarFile, List<JarFile> jarFileList) {
        jarFileList.add(jarFile);
        try {
            super.addURL(new URL("file", "", jarFile.getName()));
        } catch (MalformedURLException e1) {
            throw new RuntimeException(e1);
        }

        try {
            Enumeration<JarEntry> en = jarFile.entries();
            final String EXT_JAR = ".jar";
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                this.onLoadJar(je);
                String s = je.getName().toLowerCase(); // JarEntry name
                if (s.lastIndexOf(EXT_JAR) == s.length() - EXT_JAR.length()) {
                    JarEntryInfo inf = new JarEntryInfo(jarFile, je);
                    File file = createTempFile(inf);
                    log("Loading inner JAR %s from temp file %s", inf.jarEntry,
                            getFilename4Log(file));
                    try {
                        if (s.startsWith(this.refembedPath)) {
                            loadJar(new JarFile(file), this.lstJarFile);
                        } else if (s.startsWith(this.referencePath)) {
                            ClassLoader parent = getParent();
                            if (parent instanceof JarClassLoader) {
                                JarClassLoader jcl = (JarClassLoader) parent;
                                jcl.loadJar(new JarFile(file), jcl.lstJarFile);
                            }
                        } else if (s.startsWith(this.refoverridePath)) {
                            ClassLoader parent = getParent();
                            if (parent instanceof JarClassLoader) {
                                JarClassLoader jcl = (JarClassLoader) parent;
                                jcl.loadJar(new JarFile(file), jcl.overridesJarFile);
                            }
                        } else {
                            CJSystem.current().environment().logging()
                                    .info(String.format(
                                            "found jar %s,but it is not in reference or refemberd, so do not load",
                                            s));
                            // loadJar(new JarFile(file));
                        }
                    } catch (IOException e) {
                        throw new JarClassLoaderException(
                                "Cannot load inner JAR " + inf.jarEntry, e);
                    }
                }
            }
        } catch (JarClassLoaderException e) {
            throw new RuntimeException(
                    "ERROR on loading InnerJAR: " + e.getMessageAll());
        }
    } // loadJar()

    private JarEntryInfo findJarEntry(String sName, List<JarFile> jarFileList) {
        for (JarFile jarFile : jarFileList) {
            JarEntry jarEntry = jarFile.getJarEntry(sName);
            if (jarEntry != null) {
                return new JarEntryInfo(jarFile, jarEntry);
            }
        }
        return null;
    } // findJarEntry()

    private List<JarEntryInfo> findJarEntries(String sName) {
        List<JarEntryInfo> lst = new ArrayList<JarEntryInfo>();
        for (JarFile jarFile : lstJarFile) {
            JarEntry jarEntry = jarFile.getJarEntry(sName);
            if (jarEntry != null) {
                lst.add(new JarEntryInfo(jarFile, jarEntry));
            }
        }
        return lst;
    } // findJarEntries()

    /**
     * Finds native library entry.
     *
     * @param sLib Library name. For example for the library name "Native" the
     *             Windows returns entry "Native.dll", the Linux returns entry
     *             "libNative.so", the Mac returns entry "libNative.jnilib".
     * @return Native library entry
     */
    private JarEntryInfo findJarNativeEntry(String sLib) {
        String sName = System.mapLibraryName(sLib);
        for (JarFile jarFile : lstJarFile) {
            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory()) {
                    continue; // directory is a ZipEntry which name ends with
                    // "/"
                }
                // Example: sName is "Native.dll"
                String sEntry = je.getName(); // "Native.dll" or
                // "abc/xyz/Native.dll"
                // sName "Native.dll" could be found, for example
                // - in the path: abc/Native.dll/xyz/my.dll <-- do not load this
                // one!
                // - in the partial name: abc/aNative.dll <-- do not load this
                // one!
                String[] token = sEntry.split("/"); // the last token is library
                // name
                if (token.length > 0 && token[token.length - 1].equals(sName)) {
                    log("Loading native library '%s' found as '%s' in JAR %s",
                            sLib, sEntry, jarFile.getName());
                    return new JarEntryInfo(jarFile, je);
                }
            }
        }
        return null;
    } // findJarNativeEntry()

    /**
     * Loads class from a JAR and searches for all jar-in-jar.
     *
     * @param sClassName class to load
     * @return loaded class
     * @throws JarClassLoaderException
     */
    private Class<?> findJarClass(String sClassName, List<JarFile> jarFileList)
            throws JarClassLoaderException {
        // http://java.sun.com/developer/onlineTraining/Security/Fundamentals
        // /magercises/ClassLoader/solution/FileClassLoader.java
        Class<?> c = null;
        // Char '/' works for Win32 and Unix.
        String sName = sClassName.replace('.', '/') + ".class";
        JarEntryInfo inf = findJarEntry(sName, jarFileList);
        if (inf != null) {
            definePackage(sClassName, inf);
            byte[] a_by = inf.getJarBytes();
            try {
                c = defineClass(sClassName, a_by, 0, a_by.length, pd);
            } catch (ClassFormatError e) {
                throw new JarClassLoaderException(null, e);
            }
        }
        if (c == null) {
            throw new JarClassLoaderException(sClassName);
        }
        hmClass.put(sClassName, c);
//         System.out.println("@@@@@@@@@@@@"+c);
        return c;
    } // findJarClass()

    /**
     * The default <code>ClassLoader.defineClass()</code> does not create
     * package for the loaded class and leaves it null. Each package referenced
     * by this class loader must be created only once before the
     * <code>ClassLoader.defineClass()</code> call. The base class
     * <code>ClassLoader</code> keeps cache with created packages for reuse.
     *
     * @param sClassName class to load
     * @throws IllegalArgumentException If package name duplicates an existing package either in this
     *                                  class loader or one of its ancestors
     */
    private void definePackage(String sClassName, JarEntryInfo inf)
            throws IllegalArgumentException {
        int pos = sClassName.lastIndexOf('.');
        String sPackageName = pos > 0 ? sClassName.substring(0, pos) : "";
        if (getPackage(sPackageName) == null) {
            definePackage(sPackageName, inf.getSpecificationTitle(),
                    inf.getSpecificationVersion(), inf.getSpecificationVendor(),
                    inf.getImplementationTitle(),
                    inf.getImplementationVersion(),
                    inf.getImplementationVendor(), inf.getSealURL());
        }
    }

    // --------------------------------separator--------------------------------
    static int ______SHUTDOWN;

    /**
     * Called on shutdown for temporary files cleanup
     */
    private void shutdown() {
        // All inner JAR temporary files are marked at the time of creation
        // as deleteOnExit(). These files are not deleted if they are not
        // closed.
        for (JarFile jarFile : lstJarFile) {
            try {
                jarFile.close();
            } catch (IOException e) {
                // Ignore. In the worst case temp files will accumulate.
            }
        }
        for (JarFile jarFile : overridesJarFile) {
            try {
                jarFile.close();
            } catch (IOException e) {
                // Ignore. In the worst case temp files will accumulate.
            }
        }
        // JVM does not close handles to native libraries files
        // and temp files even marked closeOnExit() are not deleted.
        // Use special file with list of native libraries temp files
        // to delete them on next application run.
        String sPersistentFile = System.getProperty("user.home")
                + File.separator + ".JarClassLoader";
        deleteOldNative(sPersistentFile);
        persistNewNative(sPersistentFile);
    } // shutdown()

    /**
     * Deletes temporary files listed in the file. The method is called on
     * shutdown().
     *
     * @param sPersistentFile file name with temporary files list
     */
    private void deleteOldNative(String sPersistentFile) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(sPersistentFile));
            String sLine;
            while ((sLine = reader.readLine()) != null) {
                File file = new File(sLine);
                if (!file.exists()) {
                    continue; // already deleted; from command line?
                }
                if (!file.delete()) {
                    // Cannot delete, will try next time.
                    hsNativeFile.add(file);
                }
            }
        } catch (IOException e) {
            // Ignore. In the worst case temp files will accumulate.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    } // deleteOldNative()

    /**
     * Creates file with temporary files list. This list will be used to delete
     * temporary files on the next application launch. The method is called from
     * shutdown().
     *
     * @param sPersistentFile file name with temporary files list
     */
    private void persistNewNative(String sPersistentFile) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(sPersistentFile));
            for (File fileNative : hsNativeFile) {
                writer.write(fileNative.getCanonicalPath());
                writer.newLine();

                // The temporary file with native library is marked
                // as deleteOnExit() but VM does not close it and it remains
                // open.
                // Attempt to explicitly delete the file fails with "false"
                // because VM does not release the file handle.
                fileNative.delete(); // returns "false"
            }
        } catch (IOException e) {
            // Ignore. In the worst case temp files will accumulate.
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    } // persistNewNative()

    // --------------------------------separator--------------------------------
    static int ______ACCESS;

    /**
     * Checks how the application was loaded: from JAR or file system.
     *
     * @return true if application was started from JAR
     */
    public boolean isLaunchedFromJar() {
        return (lstJarFile.size() > 0);
    } // isLaunchedFromJar()

    /**
     * Returns the name of the jar file main class, or null if no "Main-Class"
     * manifest attributes was defined.
     *
     * @return main class declared in JAR's manifest
     */
    public String getManifestMainClass() {
        Attributes attr = null;
        if (isLaunchedFromJar()) {
            try {
                // The first element in array is the top level JAR
                Manifest m = lstJarFile.get(0).getManifest();
                attr = m.getMainAttributes();
            } catch (IOException e) {
            }
        }
        return (attr == null ? null
                : attr.getValue(Attributes.Name.MAIN_CLASS));
    }

    /**
     * Invokes main() method on class with provided parameters.
     *
     * @param sClass class name in form "MyClass" for default package or
     *               "com.abc.MyClass" for class in some package
     * @param args   arguments for the main() method or null
     * @throws Throwable wrapper for many exceptions thrown while
     *                   <p>
     *                   (1) main() method lookup: ClassNotFoundException,
     *                   SecurityException, NoSuchMethodException
     *                   <p>
     *                   (2) main() method launch: IllegalArgumentException,
     *                   IllegalAccessException (disabled)
     *                   <p>
     *                   (3) Actual cause of InvocationTargetException
     *                   <p>
     *                   See {@link
     *                   ://java.sun.com/developer/Books/javaprogramming
     *                   /JAR/api/jarclassloader.html} and {@link
     *                   ://java.sun.com/developer
     *                   /Books/javaprogramming/JAR/api/example
     *                   -1dot2/JarClassLoader.java}
     */
    public void invokeMain(String sClass, String[] args) throws Throwable {
        // The default is sun.misc.Launcher$AppClassLoader (run from file system
        // or JAR)
        // Thread.currentThread().setContextClassLoader(this);
        Class<?> clazz = loadClass(sClass);
        log("Launch: %s.main(); Loader: %s", sClass, clazz.getClassLoader());
        Method method = clazz.getMethod("main",
                new Class<?>[]{String[].class});

        boolean bValidModifiers = false;
        boolean bValidVoid = false;

        if (method != null) {
            method.setAccessible(true); // Disable IllegalAccessException
            int nModifiers = method.getModifiers(); // main() must be
            // "public static"
            bValidModifiers = Modifier.isPublic(nModifiers)
                    && Modifier.isStatic(nModifiers);
            Class<?> clazzRet = method.getReturnType(); // main() must be "void"
            bValidVoid = (clazzRet == void.class);
        }
        if (method == null || !bValidModifiers || !bValidVoid) {
            throw new NoSuchMethodException(
                    "The main() method in class \"" + sClass + "\" not found.");
        }

        // Invoke method.
        // Crazy cast "(Object)args" because param is: "Object... args"
        try {
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    } // invokeMain()

    // --------------------------------separator--------------------------------
    static int ______OVERRIDE;

    /**
     * Class loader JavaDoc encourages overriding findClass(String) in derived
     * class rather than overriding this method. This does not work for loading
     * classes from a JAR. Default implementation of loadClass() is able to load
     * a class from a JAR without calling findClass(). This will "infect" the
     * loaded class with a system class loader. The system class loader will be
     * used to load all dependent classes and will fail for jar-in-jar classes.
     * <p>
     * See also: http://www.cs.purdue.edu/homes/jv/smc/pubs/liang-oopsla98.pdf
     */
    @Override
    protected synchronized Class<?> loadClass(String sClassName,
                                              boolean bResolve) throws ClassNotFoundException {
        log("LOADING %s (resolve=%b)", sClassName, bResolve);

        Class<?> c = null;
        c = findLoadedClass(sClassName);
        if (c == null) {
            c = hmClass.get(sClassName);
        }
        if (c != null) {
            return c;
        }
        if (c == null && isLaunchedFromJar()) {
            try {
                c = findJarClass(sClassName, this.overridesJarFile);
                log("Loaded %s from JAR by %s", sClassName,
                        getClass().getName());
                return c;
            } catch (JarClassLoaderException e) {
                // e.printStackTrace();
                if (e.getCause() == null) {
                    log("Not found %s in JAR by %s: %s", sClassName,
                            getClass().getName(), e.getMessage());
                } else {
                    log("Error loading %s in JAR by %s: %s", sClassName,
                            getClass().getName(), e.getCause());
                }
                // keep looking...
            }
        }
        if (c != null) {
            return c;
        }
        try {
            // No need to call findLoadedClass(sClassName) because it's
            // called inside:
            ClassLoader cl = getParent();
            c = cl.loadClass(sClassName);
            if (c != null && bResolve)
                resolveClass(c);
            log("Loaded %s by %s", sClassName, cl.getClass().getName());
            if (c != null)
                return c;
        } catch (ClassNotFoundException e) {
        } finally {
            if (c == null && isLaunchedFromJar()) {
                try {
                    c = findJarClass(sClassName, this.lstJarFile);
                    log("Loaded %s from JAR by %s", sClassName,
                            getClass().getName());
                    return c;
                } catch (JarClassLoaderException e) {
                    // e.printStackTrace();
                    if (e.getCause() == null) {
                        log("Not found %s in JAR by %s: %s", sClassName,
                                getClass().getName(), e.getMessage());
                    } else {
                        log("Error loading %s in JAR by %s: %s", sClassName,
                                getClass().getName(), e.getCause());
                    }
                    // keep looking...
                }
            }
        }
        if (c == null) {
            c = findClass(sClassName);
        }
        return c;
    } // loadClass()

    /**
     * @see java.lang.ClassLoader#findResource(java.lang.String)
     */
    @Override
    public URL findResource(String sName) {
        log("findResource: %s", sName);
        if (isLaunchedFromJar()) {
            JarEntryInfo inf = findJarEntry(sName, this.overridesJarFile);
            if (inf == null) {
                inf = findJarEntry(sName, this.lstJarFile);
            }
            if (inf == null) {//当地查不着，到父查
                ClassLoader cl = getParent();
                if (cl != null) {
                    return cl.getResource(sName);
                } else {
                    return null;
                }
            }
            return inf.getURL();
        }
        return super.findResource(sName);
    }

    /**
     * @see java.lang.ClassLoader#findResources(java.lang.String)
     */
    @Override
    public Enumeration<URL> findResources(String sName) throws IOException {
        log("getResources: %s", sName);
        if (isLaunchedFromJar()) {
            List<JarEntryInfo> lstJarEntry = findJarEntries(sName);
            List<URL> lstURL = new ArrayList<URL>();
            for (JarEntryInfo inf : lstJarEntry) {
                URL url = inf.getURL();
                if (url != null) {
                    lstURL.add(url);
                }
            }
            return Collections.enumeration(lstURL);
        }
        return super.findResources(sName);
    } // getResources()

    /**
     * @see java.lang.ClassLoader#findLibrary(java.lang.String)
     */
    @Override
    protected String findLibrary(String sLib) {
        log("findLibrary: %s", sLib);
        JarEntryInfo inf = findJarNativeEntry(sLib);
        if (inf != null) {
            try {
                File fileNative = createTempFile(inf);
                log("Loading native library %s from temp file %s", inf.jarEntry,
                        getFilename4Log(fileNative));
                hsNativeFile.add(fileNative);
                return fileNative.getAbsolutePath();
            } catch (JarClassLoaderException e) {
                log("Failure to load native library %s: %s", sLib,
                        e.toString());
            }
        }
        return null;
    } // findLibrary()

    // --------------------------------separator--------------------------------
    static int ______HELPERS;

    private String getFilename4Log(File file) {
        if (logger != null) {
            try {
                // In form "C:\Documents and Settings\..."
                return file.getCanonicalPath();
            } catch (IOException e) {
                // In form "C:\DOCUME~1\..."
                return file.getAbsolutePath();
            }
        }
        return null;
    } // getFilename4Log()

    private void log(String sMsg, Object... obj) {
        if (logger != null) {
            logger.printf("JarClassLoader: " + sMsg + "\n", obj);
        }
    } // log()

    /**
     * Inner class with JAR entry information. Keeps JAR file and entry object.
     */
    private static class JarEntryInfo {
        JarFile jarFile;
        JarEntry jarEntry;
        Manifest mf; // required for package creation

        JarEntryInfo(JarFile jarFile, JarEntry jarEntry) {
            this.jarFile = jarFile;
            this.jarEntry = jarEntry;
            try {
                this.mf = jarFile.getManifest();
            } catch (IOException e) {
                // Manifest does not exist or not available
                this.mf = new Manifest();
            }
        }

        URL getURL() {
            try {
                return new URL(
                        "jar:file:" + jarFile.getName() + "!/" + jarEntry);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        String getName() {
            return jarEntry.getName().replace('/', '_');
        }

        String getSpecificationTitle() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_TITLE);
        }

        String getSpecificationVersion() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_VERSION);
        }

        String getSpecificationVendor() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_VENDOR);
        }

        String getImplementationTitle() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_TITLE);
        }

        String getImplementationVersion() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
        }

        String getImplementationVendor() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VENDOR);
        }

        URL getSealURL() {
            String seal = mf.getMainAttributes().getValue(Name.SEALED);
            if (seal != null)
                try {
                    return new URL(seal);
                } catch (MalformedURLException e) {
                    // Ignore, will return null
                }
            return null;
        }

        @Override
        public String toString() {
            return "JAR: " + jarFile.getName() + " ENTRY: " + jarEntry;
        }

        /**
         * Read JAR entry and returns byte array of this JAR entry. This is a
         * helper method to load JAR entry into temporary file.
         *
         * @return byte array for the specified JAR entry
         * @throws JarClassLoaderException
         */
        byte[] getJarBytes() throws JarClassLoaderException {
            DataInputStream dis = null;
            byte[] a_by = null;
            try {
                long lSize = jarEntry.getSize();
                if (lSize <= 0 || lSize >= Integer.MAX_VALUE) {
                    throw new JarClassLoaderException(
                            "Invalid size " + lSize + " for entry " + jarEntry);
                }
                a_by = new byte[(int) lSize];
                InputStream is = jarFile.getInputStream(jarEntry);
                dis = new DataInputStream(is);
                dis.readFully(a_by);
            } catch (IOException e) {
                throw new JarClassLoaderException(null, e);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                    }
                }
            }
            return a_by;
        } // getJarBytes()

    } // inner class JarEntryInfo

    /**
     * Inner class to handle JarClassLoader exceptions
     */
    private static class JarClassLoaderException extends Exception {
        JarClassLoaderException(String sMsg) {
            super(sMsg);
        }

        JarClassLoaderException(String sMsg, Throwable eCause) {
            super(sMsg, eCause);
        }

        String getMessageAll() {
            StringBuilder sb = new StringBuilder();
            for (Throwable e = this; e != null; e = e.getCause()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                String sMsg = e.getMessage();
                if (sMsg == null || sMsg.length() == 0) {
                    sMsg = e.getClass().getSimpleName();
                }
                sb.append(sMsg);
            }
            return sb.toString();
        }
    } // inner class JarClassLoaderException

    protected void dispose(boolean disposing) {
        if (disposing) {
            this.shutdown();
            this.hmClass.clear();
            this.hsNativeFile.clear();
            this.lstJarFile.clear();
            this.overridesJarFile.clear();
            this.disposeSuper();
            // 清除父加载器是非常错误的
            // if ((this.getParent() != null)&&(this.getParent() instanceof
            // JarClassLoader)) {
            // JarClassLoader loader = (JarClassLoader) this.getParent();
            // loader.dispose(disposing);
            // }
        }
    }

    private void disposeSuper() {
        Class<?> clazz = SystemResource.class;
        try {
            Field parent = ObjectHelper.findProperty("parent", clazz);
            Field classes = ObjectHelper.findProperty("classes", clazz);
            Field packages = ObjectHelper.findProperty("packages", clazz);

            if (parent != null) {
                parent.setAccessible(true);
                parent.set(this, null);
            }
            if (classes != null) {
                classes.setAccessible(true);
                Vector<?> v = (Vector<?>) classes.get(this);
                if (v != null)
                    v.clear();
            }
            if (packages != null) {
                packages.setAccessible(true);
                Map<?, ?> map = (Map<?, ?>) packages.get(this);
                if (map != null)
                    map.clear();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.dispose(true);
        super.finalize();
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        this.dispose(true);
    }

}
