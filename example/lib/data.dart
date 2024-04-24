import 'package:m3u8_downloader/m3u8_downloader.dart';

class DownloadItem {
  final String url;
  final String name;

  const DownloadItem({required this.url, required this.name});
}

class DownloadItems {
  static const videos = <DownloadItem>[
    DownloadItem(
        url: "https://v10.qqtvoss.com/202404/09/XVxPLecEdq3/video/index.m3u8",
        name: 'Ep8'),
    DownloadItem(
        url:
            "https://vip1.lz-cdn8.com/20220404/708_3bd3e4a0/index.m3u8?t=57122506",
        name: "one piece"),
    DownloadItem(
        url:
            "https://v10.suoni-qiyi.com/202403/12/VRXNjRsANy3/video/index.m3u8",
        name: '幕府将军 第四集'),
    DownloadItem(
        url:
            "https://s5.bfengbf.com/video/woduzishengji/%E7%AC%AC01%E9%9B%86/index.m3u8",
        name: "Solo Leveling 9")
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
