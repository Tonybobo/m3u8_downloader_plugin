import 'package:m3u8_downloader/m3u8_downloader.dart';

class DownloadItem {
  final String url;
  final String name;

  const DownloadItem({required this.url, required this.name});
}

class DownloadItems {
  static const videos = <DownloadItem>[
    DownloadItem(
        url: "https://v.gsuus.com/play/PdRG6MOe/index.m3u8",
        name: 'Killer Ep1'),
    DownloadItem(
        url: "https://v.gsuus.com/play/QdJw1oPe/index.m3u8",
        name: "Killer Ep2"),
    DownloadItem(
        url: "https://v.gsuus.com/play/xe7KmE1e/index.m3u8",
        name: 'Killer Ep3'),
    DownloadItem(
        url: "https://v.gsuus.com/play/xbo4EQjd/index.m3u8",
        name: 'Killer Ep4'),
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
