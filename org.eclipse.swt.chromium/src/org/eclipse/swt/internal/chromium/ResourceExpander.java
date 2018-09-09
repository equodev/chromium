package org.eclipse.swt.internal.chromium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import org.eclipse.swt.internal.Library;

// TODO: copied from org.eclipse.swt.internal.Library, when merged into SWT thi
// can be removed by allowing findResource to not look into internal OSGi resource url
public class ResourceExpander {

    public static final String USER_HOME;
    static final String SEPARATOR;
    static final String JAVA_LIB_PATH = "java.library.path";
    static final String SWT_LIB_PATH = "swt.library.path";
    static final String SWT_LIB_DIR;

    static {
        SEPARATOR = File.separator;
        USER_HOME = System.getProperty ("user.home");
        SWT_LIB_DIR = ".swt" + SEPARATOR + "lib" + SEPARATOR + os() + SEPARATOR + arch(); //$NON-NLS-1$ $NON-NLS-2$
    }

    static String arch() {
        String osArch = System.getProperty("os.arch"); //$NON-NLS-1$
        if (osArch.equals ("i386") || osArch.equals ("i686")) return "x86"; //$NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$
        if (osArch.equals ("amd64")) return "x86_64"; //$NON-NLS-1$ $NON-NLS-2$
        return osArch;
    }

    static String os() {
        String osName = System.getProperty("os.name"); //$NON-NLS-1$
        if (osName.equals ("Linux")) return "linux"; //$NON-NLS-1$ $NON-NLS-2$
        if (osName.equals ("AIX")) return "aix"; //$NON-NLS-1$ $NON-NLS-2$
        if (osName.equals ("Mac OS X")) return "macosx"; //$NON-NLS-1$ $NON-NLS-2$
        if (osName.startsWith ("Win")) return "win32"; //$NON-NLS-1$ $NON-NLS-2$
        return osName;
    }
    /**
     * Locates a resource located either in java library path, swt library path, or attempts to extract it from inside swt.jar file.
     * This function supports a single level subfolder, e.g SubFolder/resource.
     *
     * Dev note: (17/12/07) This has been developed and throughly tested on GTK. Designed to work on Cocoa/Win as well, but not tested.
     *
     * @param subDir  'null' or a folder name without slashes. E.g Correct: 'mysubdir',  incorrect: '/subdir/'.
     *                Platform specific Slashes will be added automatically.
     * @param resourceName e.g swt-webkitgtk
     * @param mapResourceName  true if you like platform specific mapping applied to resource name. e.g  MyLib -> libMyLib-gtk-4826.so
     */
    public static File findResource(String subDir, String resourceName, boolean mapResourceName){

        //We construct a 'maybe' subdirectory path. 'Maybe' because if no subDir given, then it's an empty string "".
                                                                                 //       subdir  e.g:  subdir
        String maybeSubDirPath = subDir != null ? subDir + SEPARATOR : "";       //               e.g:  subdir/  or ""
        String maybeSubDirPathWithPrefix = subDir != null ? SEPARATOR + maybeSubDirPath : ""; //  e.g: /subdir/  or ""
        final String finalResourceName = resourceName;

        // 1) Look for the resource in the java/swt library path(s)
        // This code commonly finds the resource if the swt project is a required project and the swt binary (for your platform)
        // project is open in your workplace  (found in the JAVA_LIBRARY_PATH) or if you're explicitly specified SWT_LIBRARY_PATH.
        {
            Function<String, File> lookForFileInPath = searchPath -> {
                String classpath = System.getProperty(searchPath);
                if (classpath != null){
                    String[] paths = classpath.split(":");
                    for (String path : paths) {
                    File file = new File(path + SEPARATOR + maybeSubDirPath + finalResourceName);
                        if (file.exists()){
                            return file;
                        }
                    }
                }
                return null;
            };
            File result = null;
            for (String path : new String[] {JAVA_LIB_PATH,SWT_LIB_PATH}) {
                result = lookForFileInPath.apply(path);
                if (result != null)
                    return result;
            }
        }

        // 3) Need to try to pull the resource out of the swt.jar.
        // Look for the resource in the user's home directory, (if already extracted in the temp swt folder. (~/.swt/lib...)
        // Extract from the swt.jar if not there already.
        {
            // Developer note:
            // To test this piece of code, you need to compile SWT into a jar and use it in a test project. E.g
            //   cd ~/git/eclipse.platform.swt.binaries/bundles/org.eclipse.swt.gtk.linux.x86_64/
            //   mvn clean verify -Pbuild-individual-bundles -Dnative=gtk.linux.x86_64
            // then ./target/ will contain org.eclipse.swt.gtk.linux.x86_64-3.106.100-SNAPSHOT.jar (and it's source),
            //  you can copy those into your test swt project and test that your resource is extracted into something like ~/.swt/...
            // Lastly, if using subDir, you need to edit the build.properties and specify the folder you wish to have included in your jar in the includes.
            File file = new File (USER_HOME + SEPARATOR +  SWT_LIB_DIR + maybeSubDirPathWithPrefix, finalResourceName);
            if (file.exists()){
                return file;
            } else { // Try to extract file from jar if not found.

                // Create temp directory if it doesn't exist
                File tempDir = new File (USER_HOME, SWT_LIB_DIR + maybeSubDirPathWithPrefix);
                if ((!tempDir.exists () || tempDir.isDirectory ())) {
                     tempDir.mkdirs ();
                }

                StringBuilder message = new StringBuilder("");
                if (extract(file.getPath(), maybeSubDirPath + finalResourceName, message)) {
                    if (file.exists()) {
                        return file;
                    }
                }
            }
        }
        throw new UnsatisfiedLinkError("Could not find resource" + resourceName +  (subDir != null ? " (in subdirectory: " + subDir + " )" : ""));
    }
    
    /**
     *  Extract file with 'mappedName' into path 'extractToFilePath'. Cleanup leftovers if extract failed.
     * @param extractToFilePath full path of where the file is to be extacted to, inc name of file,
     *                          e.g /home/USER/.swt/lib/linux/x86_64/libswt-MYLIB-gtk-4826.so
     * @param mappedName file to be searched in jar.
     * @return  true upon success, failure if something went wrong.
     */
    static boolean extract (String extractToFilePath, String mappedName, StringBuilder message) {
        FileOutputStream os = null;
        InputStream is = null;
        File file = new File(extractToFilePath);
        boolean extracted = false;
        try {
            if (!file.exists ()) {
                is = Library.class.getResourceAsStream ("/" + mappedName.replace('\\', '/')); //$NON-NLS-1$
                if (is != null) {
                    extracted = true;
                    int read;
                    byte [] buffer = new byte [4096];
                    os = new FileOutputStream (extractToFilePath);
                    while ((read = is.read (buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.close ();
                    is.close ();
                    chmod ("755", extractToFilePath);
                    return true;
                }
            }
        } catch (Throwable e) {
            try {
                if (os != null) os.close ();
            } catch (IOException e1) {}
            try {
                if (is != null) is.close ();
            } catch (IOException e1) {}
            if (extracted && file.exists ()) file.delete ();
        }
        return false;
    }
    
    static void chmod(String permision, String path) {
        if (os().equals ("win32")) return; //$NON-NLS-1$
        try {
            Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor(); //$NON-NLS-1$
        } catch (Throwable e) {}
    }
}
