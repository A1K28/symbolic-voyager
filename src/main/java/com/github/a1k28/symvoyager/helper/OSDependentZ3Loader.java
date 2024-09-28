package com.github.a1k28.symvoyager.helper;

public class OSDependentZ3Loader {
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    public static void loadZ3Library(String prefix) {
        String libraryName = getZ3LibraryName();
        System.load(prefix+libraryName);
    }

    private static boolean isWindows() {
        return OS.contains("win");
    }

    private static boolean isMac() {
        return OS.contains("mac");
    }

    private static boolean isUnix() {
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
    }

    private static String getZ3LibraryName() {
        if (isWindows()) {
            return "libz3.dll";
        } else if (isMac()) {
            if (OS_ARCH.contains("aarch64") || OS_ARCH.contains("arm")) {
                return "libz3.dylib"; // ARM-based Macs
            } else {
                return "libz3.dylib"; // Intel-based Macs
            }
        } else if (isUnix()) {
            return "libz3.so";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system");
        }
    }
}