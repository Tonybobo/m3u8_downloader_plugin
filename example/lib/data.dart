import 'package:m3u8_downloader/m3u8_downloader.dart';

class DownloadItem {
  final String url;
  final String name;

  const DownloadItem({required this.url, required this.name});
}

class DownloadItems {
  static const videos = <DownloadItem>[
    DownloadItem(
        url:
            "https://m3u.haiwaikan.com/xm3u8/d79af3e4025c3ecc75e60991f652cdf3f8e2e4fbd42e1f12762e875f8049eb159921f11e97d0da21.m3u8",
        name: 'Solo Leveling'),
    DownloadItem(
        url:
            "https://v10.suoni-qiyi.com/202403/12/VRXNjRsANy3/video/index.m3u8",
        name: '幕府将军 第四集')
  ];
}

class ItemHolder {
  final String? name;

  final TaskInfo? task;

  ItemHolder({this.name, this.task});
}

class TaskInfo {
  final String? name;
  final String? link;

  TaskInfo({this.name, this.link});

  String? taskId;
  int? progress = 0;
  DownloadTaskStatus? status = DownloadTaskStatus.undefined;
}
