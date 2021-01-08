package com.zl.util;

import java.io.File;
import java.util.Objects;

/**
 * 文件工具
 *
 * @author 郑龙
 * @date 2020/11/5 8:55
 */
public class FileUtil {
    /**
     * 判断文件是否存在
     *
     * @param fileName 文件名
     * @return true/false 存在/不存在
     */
    public static boolean contain(String fileName) {
        return new File(fileName).exists();
    }

    /**
     * 判断文件夹是否存在,不存在则创建
     *
     * @param file 文件夹路径
     */
    public static boolean judeDirExists(File file) {
        return file.exists() ? file.isDirectory() : file.mkdirs();
    }

    /**
     * 清除指定目录下的m4a缓存文件
     *
     * @param cacheFileDirPath 目录路径
     */
    public static void cleanM4aCache(String cacheFileDirPath) {
        File dir = new File(cacheFileDirPath);
        if (dir.isDirectory()) {
            //如果是文件夹
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile() && file.getName().endsWith("m4a")) {
                    //清理M4A文件
                    file.delete();
                }
            }
        }
    }
}
