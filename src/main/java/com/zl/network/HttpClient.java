package com.zl.network;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zl.constants.Url;
import com.zl.pojo.AlbumModel;
import com.zl.pojo.TrackModel;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 郑龙
 * @date 2020/11/4 16:22
 */
public class HttpClient {
    private static OkHttpClient client = new OkHttpClient();
    private static ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 搜索播放列表
     *
     * @param page 查询页码
     * @return 播放列表实体
     * @throws InterruptedException 线程问题抛出的异常
     */
    public static List<AlbumModel> doSearch(final Integer page) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<AlbumModel> resultList = new CopyOnWriteArrayList<>();

        Thread task = new Thread(() -> {
            Request request = new Request.Builder().url(Url.BASE_URL + Url.BASE_SEARCH_URI + "&page=" + page).get().build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
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

        executor.submit(task);

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
    public static List<TrackModel> queryAlbumDetailById(String id) throws IOException {
        List<TrackModel> resultList = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            Request request = new Request.Builder().get().url(Url.BASE_URL + Url.SEARCH_ALBUM_URI + "&id=" + id).build();
            Response response = client.newCall(request).execute();
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

                CountDownLatch countDownLatch = new CountDownLatch(currentList.size());
                for (TrackModel trackModel : currentList) {
                    Thread thread = new Thread(() -> {
                        try {
                            String src = HttpClient.getTrackUrl(trackModel.getTrackId());
                            trackModel.setSrc(src);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        countDownLatch.countDown();
                    });
                    executor.submit(thread);
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                resultList.addAll(currentList);
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
            Response response = client.newCall(request).execute();
            Optional.ofNullable(response.body()).map(responseBody -> {
                try {
                    return responseBody.string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).ifPresent(s -> {
                url.set(JSONObject
                        .parseObject(s)
                        .getJSONObject("data").getString("src"));
            });
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
