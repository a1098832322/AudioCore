package com.zl.constants;

/**
 * URL定义
 *
 * @author 郑龙
 * @date 2020/11/4 16:20
 */
public class Url {
    /**
     * 喜马拉雅域名
     */
    public static final String BASE_URL = "https://www.ximalaya.com";

    /**
     * 基础搜索URL
     */
    public static final String BASE_SEARCH_URI
            = "/revision/search/main?core=album&spellchecker=true&rows=20&condition=relation&device=iPhone";

    /**
     * 根据专辑id查询专辑详情(需要拼接专辑id)
     */
    public static final String SEARCH_ALBUM_URI = "/revision/play/v1/show?num=1&sort=-1&size=30&ptype=0";

    /**
     * 根据音频id获取播放地址(需要拼接音源id)
     */
    public static final String GET_AUDIO_PATH = "/revision/play/v1/audio?ptype=1";
}
