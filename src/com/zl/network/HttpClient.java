package com.zl.network;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zl.constants.Url;
import com.zl.pojo.AlbumModel;
import com.zl.pojo.TrackModel;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模拟HTTP客户端
 *
 * @author 郑龙
 * @date 2020/11/4 16:22
 */
public class HttpClient {
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * 搜索播放列表
     *
     * @param keyword 关键词
     * @param page    查询页码
     * @return 播放列表实体
     * @throws InterruptedException 线程问题抛出的异常
     */
    public static List<AlbumModel> doSearch(String keyword, final Integer page) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<AlbumModel> resultList = new CopyOnWriteArrayList<>();
        //提交查询任务
        EXECUTOR.submit(() -> {
            Response response = null;
            try {
                Request request = new Request.Builder()
                        .url(Url.BASE_URL + Url.BASE_SEARCH_URI
                                //根据关键词搜索，默认关键词为“郭德纲”
                                + "&kw=" + (StringUtils.isBlank(keyword) ? "%E9%83%AD%E5%BE%B7%E7%BA%B2" : URLEncoder.encode(keyword, "utf-8"))
                                + "&page=" + page)
                        .get().build();
                response = CLIENT.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Optional.ofNullable(response.body()).map(responseBody -> {
                try {
                    return responseBody.string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).ifPresent(s -> {
                List<AlbumModel> currentList = JSONObject.parseArray(JSON
                                .toJSONString(JSONObject
                                        .parseObject(s)
                                        .getJSONObject("data")
                                        .getJSONObject("album")
                                        .getJSONArray("docs"))
                        , AlbumModel.class);
                resultList.addAll(currentList);
            });

            countDownLatch.countDown();
        });

        countDownLatch.await();
        return resultList;
    }

    /**
     * 根据音源id获取列表详情
     *
     * @param id 列表id
     * @return 音源列表
     * @throws IOException 异常
     */
    public synchronized static List<TrackModel> queryAlbumDetailById(String id) throws IOException {
        List<TrackModel> resultList = new CopyOnWriteArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            Request request = new Request.Builder().get().url(Url.BASE_URL + Url.SEARCH_ALBUM_URI + "&id=" + id).build();

            Response response = CLIENT.newCall(request).execute();
            Optional.ofNullable(response.body()).map(responseBody -> {
                try {
                    return responseBody.string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).ifPresent(s -> {
                List<TrackModel> currentList = JSONObject.parseArray(JSON
                                .toJSONString(JSONObject
                                        .parseObject(s)
                                        .getJSONObject("data")
                                        .getJSONArray("tracksAudioPlay"))
                        , TrackModel.class);

                //遍历数据集，仅加载能播放的曲目
                CountDownLatch countDownLatch = new CountDownLatch(currentList.size());
                for (TrackModel trackModel : currentList) {
                    Thread thread = new Thread(() -> {
                        try {
                            String src = HttpClient.getTrackUrl(trackModel.getTrackId());
                            if (StringUtils.isNotBlank(src)) {
                                trackModel.setSrc(src);
                                resultList.add(trackModel);
                            } else {
                                //需要VIP的没有直接播放路径
                                //如果没有播放路径则不管。毕竟播不出来
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        countDownLatch.countDown();
                    });
                    EXECUTOR.submit(thread);
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        }

        return resultList;
    }

    /**
     * 获得播放列表的url
     *
     * @param trackId 音频id
     * @return 播放url
     * @throws IOException 异常
     */
    public static String getTrackUrl(String trackId) throws IOException {
        AtomicReference<String> url = new AtomicReference<>();
        if (StringUtils.isNotBlank(trackId)) {
            Request request = new Request.Builder().get().url(Url.BASE_URL + Url.GET_AUDIO_PATH + "&id=" + trackId).build();
            synchronized (CLIENT) {
                Response response = CLIENT.newCall(request).execute();
                Optional.ofNullable(response.body()).map(responseBody -> {
                    try {
                        return responseBody.string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).flatMap(s -> Optional.ofNullable(JSONObject
                        .parseObject(s)
                        .getJSONObject("data").getString("src"))).ifPresent(url::set);
            }
        }
        return url.get();
    }

    /**
     * 下载音频文件，支持断点续传
     *
     * @param url              url
     * @param musicName        名称
     * @param downloadListener 下载监听器
     * @param startsPoint      起始点
     */
    public static void download(String url, String musicName, final DownloadListener downloadListener, final long startsPoint) {
        Request request = new Request.Builder()
                .url(url)
                //断点续传
                .header("RANGE", "bytes=" + startsPoint + "-")
                .build();

        // 重写ResponseBody监听请求
        Interceptor interceptor = chain -> {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                    .body(new DownloadResponseBody(originalResponse, startsPoint, downloadListener))
                    .build();
        };

        OkHttpClient.Builder dlOkhttp = new OkHttpClient.Builder()
                .addNetworkInterceptor(interceptor);


        // 发起请求
        dlOkhttp.build().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                long length = response.body().contentLength();
                if (length == 0) {
                    // 说明文件已经下载完，直接跳转安装就好
                    downloadListener.complete(String.valueOf(getFile(musicName).getAbsoluteFile()));
                    return;
                }
                downloadListener.start(length + startsPoint);
                // 保存文件到本地
                InputStream is = null;
                RandomAccessFile randomAccessFile = null;
                BufferedInputStream bis = null;

                byte[] buff = new byte[2048];
                int len = 0;
                try {
                    is = Objects.requireNonNull(response.body()).byteStream();
                    bis = new BufferedInputStream(is);

                    File file = getFile(musicName);
                    // 随机访问文件，可以指定断点续传的起始位置
                    randomAccessFile = new RandomAccessFile(file, "rwd");
                    randomAccessFile.seek(startsPoint);
                    while ((len = bis.read(buff)) != -1) {
                        randomAccessFile.write(buff, 0, len);
                    }

                    // 下载完成
                    downloadListener.complete(String.valueOf(file.getAbsoluteFile()));
                } catch (Exception e) {
                    e.printStackTrace();
                    downloadListener.loadFail(e.getMessage());
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (bis != null) {
                            bis.close();
                        }
                        if (randomAccessFile != null) {
                            randomAccessFile.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }


        });
    }

    /**
     * 获得临时文件
     *
     * @param name 文件名
     * @return 文件
     */
    private static File getFile(String name) {
        return new File(name);
    }
}
