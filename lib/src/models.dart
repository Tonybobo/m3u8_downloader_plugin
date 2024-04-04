@pragma('vm:entry-point')
enum DownloadTaskStatus {
  undefined,
  enqueued,
  running,
  complete,
  failed,
  canceled,
  paused;

  factory DownloadTaskStatus.fromInt(int value) {
    switch (value) {
      case 0:
        return DownloadTaskStatus.undefined;
      case 1:
        return DownloadTaskStatus.enqueued;
      case 2:
        return DownloadTaskStatus.running;
      case 3:
        return DownloadTaskStatus.complete;
      case 4:
        return DownloadTaskStatus.failed;
      case 5:
        return DownloadTaskStatus.canceled;
      case 6:
        return DownloadTaskStatus.paused;
      default:
        throw ArgumentError('Invalid Value: $value');
    }
  }
}

class DownloadTask {
  final String taskId;
  final DownloadTaskStatus status;
  final int progress;
  final String url;
  final String filename;
  final int timeCreated;

  DownloadTask({
    required this.taskId,
    required this.status,
    required this.progress,
    required this.url,
    required this.filename,
    required this.timeCreated,
  });

  @override
  String toString() =>
      'DownloadTask(taskId: $taskId , status: $status , progress: $progress , url: $url, filename: $filename , timeCreated: $timeCreated ,)';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    return other is DownloadTask &&
        other.taskId == taskId &&
        other.status == status &&
        other.progress == progress &&
        other.url == url &&
        other.filename == filename &&
        other.timeCreated == timeCreated;
  }

  @override
  int get hashCode {
    return Object.hash(taskId, status, progress, url, filename, timeCreated);
  }
}
