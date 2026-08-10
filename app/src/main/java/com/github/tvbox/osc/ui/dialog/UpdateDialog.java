package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.github.tvbox.osc.R;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;

import java.io.File;

/**
 * 更新弹窗：展示版本信息，下载安装包并显示进度
 */
public class UpdateDialog extends BaseDialog {

    private static final String DOWNLOAD_TAG = "tvbox_update_download";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final String mVersionName;
    private final String mUpdateMsg;
    private final String mApkUrl;

    private TextView btnUpdate;
    private TextView btnCancel;
    private View llProgress;
    private ProgressBar progressBar;
    private TextView tvProgress;

    public UpdateDialog(Context context, String versionName, String updateMsg, String apkUrl) {
        super(context);
        this.mVersionName = versionName;
        this.mUpdateMsg = updateMsg;
        this.mApkUrl = apkUrl;
        setContentView(R.layout.dialog_update);
        initView();
    }

    private void initView() {
        ((TextView) findViewById(R.id.tvTitle)).setText("发现新版本 v" + mVersionName);
        TextView tvMsg = findViewById(R.id.tvMsg);
        tvMsg.setText(mUpdateMsg == null || mUpdateMsg.isEmpty() ? "新版本已发布，请更新。" : mUpdateMsg);
        llProgress = findViewById(R.id.llProgress);
        progressBar = findViewById(R.id.pbUpdate);
        tvProgress = findViewById(R.id.tvProgress);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkGo.getInstance().cancelTag(DOWNLOAD_TAG);
                dismiss();
            }
        });
    }

    private void startDownload() {
        btnUpdate.setEnabled(false);
        btnCancel.setText("取消");
        llProgress.setVisibility(View.VISIBLE);
        tvProgress.setText("0%");
        File dir = getContext().getCacheDir();
        OkGo.<File>get(mApkUrl)
                .tag(DOWNLOAD_TAG)
                .execute(new FileCallback(dir.getAbsolutePath(), "tvbox_update.apk") {
                    @Override
                    public void onSuccess(Response<File> response) {
                        final File file = response.body();
                        if (file != null && file.exists() && file.length() > 0) {
                            dismiss();
                            installApk(file);
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvProgress.setText("下载失败");
                                    btnUpdate.setEnabled(true);
                                }
                            });
                        }
                    }

                    @Override
                    public void downloadProgress(Progress progress) {
                        super.downloadProgress(progress);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                int p = (int) (progress.fraction * 100);
                                progressBar.setProgress(p);
                                tvProgress.setText(p + "%");
                            }
                        });
                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvProgress.setText("下载失败");
                                btnUpdate.setEnabled(true);
                            }
                        });
                    }
                });
    }

    private void installApk(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "安装失败，请手动安装", Toast.LENGTH_SHORT).show();
        }
    }
}
