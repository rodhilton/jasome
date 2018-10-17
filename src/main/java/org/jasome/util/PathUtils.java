package org.jasome.util;

import java.io.File;

public class PathUtils {

    public static String toUnixPath(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public static String toSystemPath(String path) {
        if(File.separator.equals("\\")) {
            return path.replaceAll("/", "\\\\");
        } else {
            return path.replaceAll("/", File.separator);
        }
    }

}
