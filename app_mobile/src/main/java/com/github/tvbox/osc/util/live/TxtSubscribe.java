package com.github.tvbox.osc.util.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TxtSubscribe {
    public static void parse(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        if (isM3U(str)) {
            parseM3U(linkedHashMap, str);
            return;
        }
        ArrayList<String> arrayList;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String readLine = bufferedReader.readLine();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap3 = linkedHashMap2;
            while (readLine != null) {
                if (readLine.trim().isEmpty()) {
                    readLine = bufferedReader.readLine();
                } else {
                    String[] split = readLine.split(",");
                    if (split.length < 2) {
                        readLine = bufferedReader.readLine();
                    } else {
                        if (readLine.contains("#genre#")) {
                            String trim = split[0].trim();
                            if (!linkedHashMap.containsKey(trim)) {
                                linkedHashMap3 = new LinkedHashMap<>();
                                linkedHashMap.put(trim, linkedHashMap3);
                            } else {
                                linkedHashMap3 = linkedHashMap.get(trim);
                            }
                        } else {
                            String trim2 = split[0].trim();
                            for (String str2 : split[1].trim().split("#")) {
                                String trim3 = str2.trim();
                                if (!trim3.isEmpty() && (trim3.startsWith("http") || trim3.startsWith("rtp") || trim3.startsWith("rtsp") || trim3.startsWith("rtmp"))) {
                                    if (!linkedHashMap3.containsKey(trim2)) {
                                        arrayList = new ArrayList<>();
                                        linkedHashMap3.put(trim2, arrayList);
                                    } else {
                                        arrayList = linkedHashMap3.get(trim2);
                                    }
                                    if (!arrayList.contains(trim3)) {
                                        arrayList.add(trim3);
                                    }
                                }
                            }
                        }
                        readLine = bufferedReader.readLine();
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) {
                return;
            }
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Throwable unused) {
        }
    }

    // 检测直播列表是否为 m3u 格式（含 #EXTM3U 头）
    public static boolean isM3U(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String trim = line.trim();
                if (trim.isEmpty()) {
                    continue;
                }
                if (trim.startsWith("#EXTM3U")) {
                    return true;
                }
                if (!trim.startsWith("#")) {
                    break;
                }
            }
        } catch (Throwable unused) {
        }
        return false;
    }

    // 解析 m3u / m3u8 格式节目列表
    // 示例：
    // #EXTM3U
    // #EXTINF:-1 tvg-id="..." tvg-logo="..." group-title="央视",CCTV1
    // #EXTVLCOPT:http-user-agent=Mozilla/5.0 ...
    // http://xxx/live.m3u8
    // #EXTGRP:央视
    public static void parseM3U(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String line;
            String currentGroup = null;
            String currentName = null;
            String currentUa = null;
            java.util.regex.Pattern groupPattern = java.util.regex.Pattern.compile("group-title\\s*=\\s*\"([^\"]*)\"");
            while ((line = bufferedReader.readLine()) != null) {
                String trim = line.trim();
                if (trim.isEmpty()) {
                    continue;
                }
                if (trim.startsWith("#EXTM3U")) {
                    continue;
                }
                if (trim.startsWith("#EXTINF:")) {
                    // 新频道,重置自定义UA
                    currentUa = null;
                    // 分组
                    java.util.regex.Matcher matcher = groupPattern.matcher(trim);
                    if (matcher.find()) {
                        String group = matcher.group(1).trim();
                        if (!group.isEmpty()) {
                            currentGroup = group;
                        }
                    }
                    // 频道名称取最后一个逗号后的内容
                    int idx = trim.lastIndexOf(',');
                    if (idx >= 0 && idx + 1 < trim.length()) {
                        currentName = trim.substring(idx + 1).trim();
                    }
                } else if (trim.startsWith("#EXTVLCOPT:")) {
                    // 读取 http-user-agent 作为该频道的播放请求头
                    String opt = trim.substring("#EXTVLCOPT:".length()).trim();
                    int uaIdx = opt.toLowerCase().indexOf("http-user-agent=");
                    if (uaIdx >= 0) {
                        String ua = opt.substring(uaIdx + "http-user-agent=".length()).trim();
                        if (ua.length() > 1 && ua.startsWith("\"") && ua.endsWith("\"")) {
                            ua = ua.substring(1, ua.length() - 1);
                        }
                        if (!ua.isEmpty()) {
                            currentUa = ua;
                        }
                    }
                } else if (trim.startsWith("#EXTGRP:")) {
                    // 旧式 m3u 的分组指令
                    String group = trim.substring(8).trim();
                    if (!group.isEmpty()) {
                        currentGroup = group;
                    }
                } else if (trim.startsWith("#")) {
                    // 其它注释行忽略
                    continue;
                } else {
                    // 地址行
                    if (currentName != null && !currentName.isEmpty()
                            && (trim.startsWith("http") || trim.startsWith("rtp") || trim.startsWith("rtsp") || trim.startsWith("rtmp"))) {
                        String url = trim;
                        // 未定义 http-user-agent 的频道不追加,播放时使用默认请求头
                        if (currentUa != null && !currentUa.isEmpty() && !url.contains("@User-Agent=")) {
                            url = url + "@User-Agent=" + currentUa;
                        }
                        String group = (currentGroup != null && !currentGroup.isEmpty()) ? currentGroup : "未分组";
                        LinkedHashMap<String, ArrayList<String>> groupMap = linkedHashMap.get(group);
                        if (groupMap == null) {
                            groupMap = new LinkedHashMap<>();
                            linkedHashMap.put(group, groupMap);
                        }
                        ArrayList<String> arrayList = groupMap.get(currentName);
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                            groupMap.put(currentName, arrayList);
                        }
                        if (!arrayList.contains(url)) {
                            arrayList.add(url);
                        }
                    }
                }
            }
            bufferedReader.close();
        } catch (Throwable unused) {
        }
    }

    public static JsonArray live2JsonArray(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap) {
        JsonArray jsonarr = new JsonArray();
        for (String str : linkedHashMap.keySet()) {
            JsonArray jsonarr2 = new JsonArray();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = linkedHashMap.get(str);
            if (!linkedHashMap2.isEmpty()) {
                for (String str2 : linkedHashMap2.keySet()) {
                    ArrayList<String> arrayList = linkedHashMap2.get(str2);
                    if (!arrayList.isEmpty()) {
                        JsonArray jsonarr3 = new JsonArray();
                        for (int i = 0; i < arrayList.size(); i++) {
                            jsonarr3.add(arrayList.get(i));
                        }
                        JsonObject jsonobj = new JsonObject();
                        try {
                            jsonobj.addProperty("name", str2);
                            jsonobj.add("urls", jsonarr3);
                        } catch (Throwable e) {
                        }
                        jsonarr2.add(jsonobj);
                    }
                }
                JsonObject jsonobj2 = new JsonObject();
                try {
                    jsonobj2.addProperty("group", str);
                    jsonobj2.add("channels", jsonarr2);
                } catch (Throwable e) {
                }
                jsonarr.add(jsonobj2);
            }
        }
        return jsonarr;
    }
}
