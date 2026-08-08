# TVBox

TVBox 是一款开源的 Android 电视盒视频播放应用，支持点播、直播、多线路播放，可通过 JSON 接口配置自定义数据源。本项目基于 [takagen99/Box](https://github.com/takagen99/Box) 二次开发。

## 功能特性

- 📺 点播 + 直播 + 回看一体化
- 🔍 全站搜索、快速搜索、搜索历史
- 🕘 观看历史（可设为首页默认显示）
- ⭐ 收藏、推送播放
- 🎬 多播放器支持：系统 / IJK / EXO，支持硬解软解切换
- 📡 多线路、多解析、自定义 UA / Referer / Cookie
- 🔄 JSON 接口配置数据源，支持自定义站点、直播源、解析规则
- 🖼️ 自定义开屏图、背景图、应用图标
- 🛰️ 投屏 / 推送播放

## 构建

### 环境要求

- Android SDK（本项目使用 `targetSdk 28`，`minSdk 19`）
- JDK 11
- Gradle 7.5

### 构建变体

| 变体 | 架构 | 说明 |
| --- | --- | --- |
| `java` | armeabi-v7a + arm64-v8a | 纯 Java，双架构 |
| `java32` | armeabi-v7a | 纯 Java，仅 32 位 |
| `java64` | arm64-v8a | 纯 Java，仅 64 位 |
| `python` | armeabi-v7a + arm64-v8a | 支持 Python 爬虫，双架构 |
| `python32` | armeabi-v7a | 支持 Python 爬虫，仅 32 位 |
| `python64` | arm64-v8a | 支持 Python 爬虫，仅 64 位 |

```bash
# 以纯 Java 双架构 Release 为例
./gradlew assembleJavaRelease

# 产物位于 app/build/outputs/apk/<变体>/release/
```

## 数据源配置

应用内"设置 → 配置"可填入远程 JSON 接口地址，接口格式示例如下：

```json
{
	"spider": "./your.jar",
	"wallpaper": "./api/img",
	"sites": [],
	"parses": [],
	"hosts": [
		"cache.ott.ystenlive.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com",
		"cache.ott.bestlive.itv.cmvideo.cn=ip"
	],
	"lives": [],
	"rules": [],
	"doh": [
		{
			"name": "騰訊",
			"url": "https://doh.pub/dns-query"
		},
		{
			"name": "阿里",
			"url": "https://dns.alidns.com/dns-query"
		},
		{
			"name": "360",
			"url": "https://doh.360.cn/dns-query"
		}
	]
}
```

### 配置字段说明

| 字段 | 说明 |
| --- | --- |
| `spider` | 爬虫 jar 包路径 |
| `wallpaper` | 壁纸接口 |
| `sites` | 站点配置（`searchable`: 搜索开关 0 关 1 开；`filterable`: 首页可选 0 否 1 是；`playerType`: 播放器 0 系统 1 IJK 2 EXO） |
| `parses` | 解析接口（0 嗅探自带播放器 1 解析返回直链） |
| `lives` | 直播配置（`ua`: 自定义 UA；`epg`: 节目单地址；`logo`: 台标地址） |
| `rules` | 爬虫替换规则 |
| `doh` | DNS over HTTPS 服务器列表 |

## 免责声明

本项目仅供技术学习与交流使用，请勿用于任何非法用途。所有数据源均来自互联网，版权归原作者所有，使用者请遵守当地法律法规。

## 许可

本项目遵循开源协议，代码仅用于学习研究，请遵守相应开源协议及原作者约定。
