package com.zl.pojo;

/**
 * 音频model
 *
 * @author 郑龙
 * @date 2020/11/4 17:08
 */
public class TrackModel {
    /**
     * 音源名
     */
    private String trackName;

    /**
     * 音源id
     */
    private String trackId;

    /**
     * 音源路径
     */
    private String src;

    /**
     * 去除一些不合法的中文字符
     *
     * @return 文件名
     */
    public String getTrackName() {
        return trackName.replaceAll("《", "").replaceAll("》", "").replaceAll(" ", "-");
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    @Override
    public String toString() {
        return trackName;
    }
}
