package com.github.tvbox.osc.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.ui.dialog.UpdateProgressDialog;
import com.github.tvbox.osc.util.urlhttp.CallBackUtil;
import com.github.tvbox.osc.util.urlhttp.UrlHttpUtil;
import com.lxj.xpopup.XPopup;

import org.json.JSONObject;

import java.io.File;

/**
 * 应用更新检测
 * 更新说明文件: https://tv.jink.top/tvbox/releases/mobile/version.json
 * 格式: {"versionCode":xx,"versionName":"x.x.x","updateLog":"更新说明","url":"apk下载地址"}
 */
public class UpdateUtil {

    public static final String UPDATE_URL = "https://tv.jink.top/tvbox/releases/mobile/version.json";

    /**
     * 检测更新
     *
     * @param context 上下文
     * @param silent  静默模式(启动自动检测): 无更新/网络失败时不提示
     */
    public static void checkUpdate(final Context context, final boolean silent) {
        UrlHttpUtil.get(UPDATE_URL, new CallBackUtil.CallBackString() {
            @Override
            public void onFailure(int code, String errorMessage) {
                if (!silent) {
                    ToastUtils.showShort("检查更新失败");
                }
            }

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    int newVersionCode = obj.optInt("versionCode", 0);
                    String newVersionName = obj.optString("versionName", "");
                    String updateLog = obj.optString("updateLog", "");
                    String downloadUrl = obj.optString("url", "");
                    if (newVersionCode > BuildConfig.VERSION_CODE && !downloadUrl.isEmpty()) {
                        showUpdateDialog(context, newVersionName, updateLog, downloadUrl);
                    } else {
                        if (!silent) {
                            ToastUtils.showShort("当前已是最新版本");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!silent) {
                        ToastUtils.showShort("检查更新失败");
                    }
                }
            }
        });
    }

    /**
     * 弹出更新对话框
     */
    private static void showUpdateDialog(final Context context, String versionName, String updateLog, final String downloadUrl) {
        String content = TextUtils.isEmpty(updateLog) ? "点击立即更新" : updateLog;
        new XPopup.Builder(context)
                .asConfirm("发现新版本 v" + versionName,
                        content,
                        "暂不更新",
                        "立即更新",
                        () -> downloadAndInstall(context, downloadUrl),
                        null,
                        true)
                .show();
    }

    /**
     * 使用系统 DownloadManager 下载,弹窗显示进度,完成后自动拉起安装
     */
    private static void downloadAndInstall(final Context context, final String downloadUrl) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null || !dir.exists()) {
                dir.mkdirs();
            }
            final String fileName = "YingHe_update.apk";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("影盒 更新");
            request.setDescription("正在下载新版本...");
            // 注意: 不调用 setNotificationVisibility, 保持默认 VISIBILITY_VISIBLE(0),
            // 部分设备的 DownloadManager 会拒绝 HIDDEN(=2) 等值
            request.setAllowedOverMetered(true);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                ToastUtils.showShort("下载失败");
                return;
            }
            final long downloadId = dm.enqueue(request);
            ToastUtils.showShort("开始下载");

            // 弹窗显示下载进度
            final UpdateProgressDialog progressDialog = new UpdateProgressDialog(context);
            new XPopup.Builder(context).asCustom(progressDialog).show();

            // 轮询 DownloadManager 查询下载进度
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable progressRunnable = new Runnable() {
                @Override
                public void run() {
                    int status = -1;
                    int percent = 0;
                    try {
                        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
                        Cursor cursor = dm.query(query);
                        if (cursor != null && cursor.moveToFirst()) {
                            status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                            long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            if (total > 0) {
                                percent = (int) (downloaded * 100 / total);
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        progressDialog.dismiss();
                        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                        installApk(context, apkFile);
                        return;
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        progressDialog.dismiss();
                        ToastUtils.showShort("下载失败,请重试");
                        return;
                    }
                    progressDialog.setProgress(percent);
                    handler.postDelayed(this, 300);
                }
            };
            handler.post(progressRunnable);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShort("开始下载失败");
        }
    }

    /**
     * 安装 APK
     */
    private static void installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            ToastUtils.showShort("下载失败,请到下载目录手动安装");
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShort("安装失败,请到下载目录手动安装");
        }
    }
}
