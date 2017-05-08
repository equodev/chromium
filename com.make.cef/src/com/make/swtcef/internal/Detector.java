/*
 * Copyright 2014 Trustin Heuiseung Lee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.make.swtcef.internal;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Detector {

    public static final String DETECTED_NAME = "os.detected.name";
    public static final String DETECTED_ARCH = "os.detected.arch";
    public static final String DETECTED_VERSION = "os.detected.version";
    public static final String DETECTED_VERSION_MAJOR = DETECTED_VERSION + ".major";
    public static final String DETECTED_VERSION_MINOR = DETECTED_VERSION + ".minor";
    public static final String DETECTED_CLASSIFIER = "os.detected.classifier";
    public static final String DETECTED_RELEASE = "os.detected.release";
    public static final String DETECTED_RELEASE_VERSION = DETECTED_RELEASE + ".version";
    public static final String DETECTED_RELEASE_LIKE_PREFIX = DETECTED_RELEASE + ".like.";

    private static final String UNKNOWN = "unknown";

    private static final Pattern VERSION_REGEX = Pattern.compile("((\\d+)\\.(\\d+)).*");

    public void detect(Properties props, List<String> classifierWithLikes) {
        final Properties allProps = new Properties(System.getProperties());
        allProps.putAll(props);

        final String osName = allProps.getProperty("os.name");
        final String osArch = allProps.getProperty("os.arch");
        final String osVersion = allProps.getProperty("os.version");

        final String detectedName = normalizeOs(osName);
        final String detectedArch = normalizeArch(osArch);

        setProperty(props, DETECTED_NAME, detectedName);
        setProperty(props, DETECTED_ARCH, detectedArch);

        Matcher versionMatcher = VERSION_REGEX.matcher(osVersion);
        if (versionMatcher.matches()) {
            setProperty(props, DETECTED_VERSION, versionMatcher.group(1));
            setProperty(props, DETECTED_VERSION_MAJOR, versionMatcher.group(2));
            setProperty(props, DETECTED_VERSION_MINOR, versionMatcher.group(3));
        }

        // Assume the default classifier, without any os "like" extension.
        String detectedClassifier = detectedName + '-' + detectedArch;

        // For Linux systems, add additional properties regarding details of the OS.
        setProperty(props, DETECTED_CLASSIFIER, detectedClassifier);
    }

    private void setProperty(Properties props, String name, String value) {
        props.setProperty(name, value);
        System.setProperty(name, value);
    }

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("aix")) {
            return "aix";
        }
        if (value.startsWith("hpux")) {
            return "hpux";
        }
        if (value.startsWith("os400")) {
            // Avoid the names such as os4000
            if (value.length() <= 5 || !Character.isDigit(value.charAt(5))) {
                return "os400";
            }
        }
        if (value.startsWith("linux")) {
            return "linux";
        }
        if (value.startsWith("macosx") || value.startsWith("osx")) {
            return "osx";
        }
        if (value.startsWith("freebsd")) {
            return "freebsd";
        }
        if (value.startsWith("openbsd")) {
            return "openbsd";
        }
        if (value.startsWith("netbsd")) {
            return "netbsd";
        }
        if (value.startsWith("solaris") || value.startsWith("sunos")) {
            return "sunos";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }

        return UNKNOWN;
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.matches("^(ia64|itanium64)$")) {
            return "itanium_64";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(value)) {
            return "ppcle_64";
        }
        if ("s390".equals(value)) {
            return "s390_32";
        }
        if ("s390x".equals(value)) {
            return "s390_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

}
