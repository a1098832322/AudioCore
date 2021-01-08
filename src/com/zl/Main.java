package com.zl;

import com.zl.audio.ConvertingAnyAudioToMp3_Example2;
import com.zl.network.DownloadListener;
import com.zl.network.HttpClient;
import com.zl.player.MusicPlayer;
import com.zl.pojo.TrackModel;
import com.zl.util.FileUtil;

import java.io.File;
import java.util.List;

/**
 * 主启动类
 *
 * @author 郑龙
 * @date 2020/11/4 16:09
 */
public class Main {
    public static void main(String[] args) throws Exception {


        //HttpClient.doSearch().forEach(System.out::println);

        List<TrackModel> list = HttpClient.queryAlbumDetailById("9723091");


        list.forEach(System.out::println);

        TrackModel trackModel = list.get(2);

        String downloadFileName = "C:/temp/" + trackModel.getTrackName() + ".m4a";
        String playFileName = "C:/temp/" + trackModel.getTrackName() + ".mp3";

        if (!FileUtil.contain(playFileName)) {
            //不存在则下载
            HttpClient.download(trackModel.getSrc(), downloadFileName,
                    new DownloadListener() {
                        @Override
                        public void start(long max) {
                            System.out.println("开始下载");
                        }

                        @Override
                        public void loading(int progress) {

                        }

                        @Override
                        public void complete(String path) {
                            System.out.println("下载完成！");
                            //M4A转码MP3
                            System.out.println("开始转码");
                            ConvertingAnyAudioToMp3_Example2 example2 = new ConvertingAnyAudioToMp3_Example2();

                            example2.convertingAnyAudioToMp3WithAProgressListener(new File(path)
                                    , new File(playFileName));
                            System.out.println("转码完成！");
                            MusicPlayer player = MusicPlayer.getInstancePlayer();
                            player.loadMusicSrc(playFileName);
                            player.openMusic();
                        }

                        @Override
                        public void fail(int code, String message) {

                        }

                        @Override
                        public void loadFail(String message) {

                        }
                    }, 0L);
        } else {
            //存在文件则直接播放
            MusicPlayer player = MusicPlayer.getInstancePlayer();
            player.loadMusicSrc(playFileName);
            player.openMusic();
        }
    }
}
