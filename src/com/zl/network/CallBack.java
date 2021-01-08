package com.zl.network;

/**
 * 回调方法
 *
 * @author 郑龙
 * @date 2020/10/9 15:09
 */
public interface CallBack<T, R> {
    /**
     * 回调方法
     *
     * @param result 执行结果
     * @return 返回值
     */
    R callback(T result);
}
