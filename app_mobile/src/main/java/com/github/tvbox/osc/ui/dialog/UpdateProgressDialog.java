package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

/**
 * 更新下载进度弹窗
 */
public class UpdateProgressDialog extends CenterPopupView {

    public UpdateProgressDialog(@NonNull @NotNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_update_progress;
    }

    /**
     * 更新下载进度
     *
     * @param percent 百分比 0-100
     */
    public void setProgress(int percent) {
        if (percent > 100) percent = 100;
        ProgressBar pb = findViewById(R.id.pb_progress);
        TextView tv = findViewById(R.id.tv_percent);
        if (pb != null) pb.setProgress(percent);
        if (tv != null) tv.setText(percent + "%");
    }
}
