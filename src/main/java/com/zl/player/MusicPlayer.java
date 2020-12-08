package com.zl.player;

import com.sun.media.jfxmedia.events.PlayerTimeListener;
import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.audio.player.Player;
import com.tulskiy.musique.audio.player.PlayerEvent;
import com.tulskiy.musique.audio.player.PlayerListener;
import com.tulskiy.musique.model.Track;
import com.tulskiy.musique.system.TrackIO;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * 基于<pre>tulskiy.musique</pre>的播放器
 *
 * @author 郑龙
 * @date 2020/10/12 9:20
 */
public class MusicPlayer {
    /**
     * PlayerHolder
     */
    private static class PlayerHolder {
        private static final Player PLAYER = new Player();
    }

    /**
     * 获取真实的player本体
     *
     * @return player本体
     */
    public Player getCurrentPlayer() {
        return PlayerHolder.PLAYER;
    }

    /**
     * 播放器内核
     */
    private static MusicPlayer player = null;

    /**
     * 音乐加载情况
     */
    private boolean loadFlag = false;

    /**
     * music track
     */
    private Track track = null;

    /**
     * private
     */
    private MusicPlayer() {
    }

    /**
     * getInstance
     *
     * @return com.zl.player instance
     */
    public static MusicPlayer getInstancePlayer() {
        if (player == null) {
            player = new MusicPlayer();
        }

        return player;
    }

    /**
     * 添加事件监听器
     *
     * @param listener 监听器
     */
    public void addListener(PlayerListener listener) {
        PlayerHolder.PLAYER.addListener(listener);
    }

    /**
     * 判断是否已生成示例
     *
     * @return true/false
     */
    private boolean isPlayerEffective() {
        return player != null;
    }

    /**
     * 是否正在播放
     *
     * @return true/false
     */
    private boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * 加载音乐
     *
     * @param path 音乐文件路径
     * @return 加载成功/失败
     */
    public boolean loadMusicSrc(String path) {
        if (StringUtils.isNotBlank(path)) {
            track = null;
            try {
                File songFile = new File(path);
                AudioFileReader audioFileReader = TrackIO
                        .getAudioFileReader(songFile.getName());
                track = audioFileReader.read(songFile);
                return track != null;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 打开
     */
    public void openMusic() {
        if (isPlayerEffective() && track != null) {
            PlayerHolder.PLAYER.open(track);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        if (isPlayerEffective()) {
            PlayerHolder.PLAYER.stop();
        }
    }

    /**
     * 开始播放
     */
    public void play() {
        if (isPlayerEffective() && PlayerHolder.PLAYER.isPaused()) {
            PlayerHolder.PLAYER.pause();
        } else if (isPlayerEffective() && PlayerHolder.PLAYER.isStopped()) {
            PlayerHolder.PLAYER.play();
        }

    }

    /**
     * 暂停
     */
    public void pause() {
        if (isPlayerEffective() && PlayerHolder.PLAYER.isPlaying()) {
            PlayerHolder.PLAYER.pause();
        }
    }
}
