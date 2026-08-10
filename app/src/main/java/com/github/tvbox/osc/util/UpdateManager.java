package com.github.tvbox.osc.util;

import android.content.Context;
import android.widget.Toast;

import com.github.tvbox.osc.ui.dialog.UpdateDialog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

/**
 * 版本更新检测：读取远程 version.json 对比本地版本
 */
public class UpdateManager {

    private static final String UPDATE_URL = "https://tv.jink.top/tvbox/releases/tv/version.json";

    public static void checkUpdate(final Context context, final boolean manual) {
        OkGo.<String>get(UPDATE_URL)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            String body = response.body();
                            if (body == null || body.isEmpty()) {
                                if (manual) toast(context, "检测更新失败");
                                return;
                            }
                            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                            int serverCode = json.get("versionCode").getAsInt();
                            String versionName = json.get("versionName").getAsString();
                            String updateMsg = json.has("updateMsg") ? json.get("updateMsg").getAsString() : "";
                            String apkUrl = json.get("apkUrl").getAsString();
                            int localCode = DefaultConfig.getAppVersionCode(context);
                            if (serverCode > localCode) {
                                UpdateDialog dialog = new UpdateDialog(context, versionName, updateMsg, apkUrl);
                                dialog.show();
                            } else if (manual) {
                                toast(context, "已是最新版本");
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                            if (manual) toast(context, "检测更新失败");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        if (manual) toast(context, "检测更新失败，请检查网络");
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
