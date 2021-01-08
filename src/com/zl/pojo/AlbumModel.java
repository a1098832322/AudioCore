package com.zl.pojo;

/**
 * 专辑model
 *
 * @author 郑龙
 * @date 2020/11/4 16:27
 */
public class AlbumModel {
    /**
     * 专辑名
     */
    private String title;

    /**
     * 专辑id
     */
    private String albumId;

    /**
     * 乐曲名称
     */
    private String albumName;

    /**
     * 播放url
     */
    private String src;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    @Override
    public String toString() {
        return title;
    }
}
