package com.zl.network;

/**
 * 下载监听
 *
 * @author 郑龙
 * @date 2020/10/9 15:24
 */
public interface DownloadListener {
    /**
     * 开始下载
     */
    void start(long max);

    /**
     * 正在下载
     */
    void loading(int progress);

    /**
     * 下载完成
     */
    void complete(String path);

    /**
     * 请求失败
     */
    void fail(int code, String message);

    /**
     * 下载过程中失败
     */
    void loadFail(String message);
}
