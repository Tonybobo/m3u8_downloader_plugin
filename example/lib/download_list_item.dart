import 'package:flutter/material.dart';
import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:m3u8_downloader_example/data.dart';

class DownloadListItem extends StatelessWidget {
  const DownloadListItem(
      {super.key, this.data, this.onTap, this.onCancel, this.onActionTap});

  final ItemHolder? data;
  final void Function(TaskInfo?)? onTap;
  final void Function(TaskInfo)? onActionTap;
  final void Function(TaskInfo)? onCancel;

  Widget? _buildTrailing(TaskInfo task) {
    switch (task.status) {
      case DownloadTaskStatus.undefined:
        return IconButton(
          onPressed: () => onActionTap?.call(task),
          constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
          icon: const Icon(Icons.file_download),
          tooltip: 'Start',
        );
      case DownloadTaskStatus.running:
        return Row(
          children: [
            Text('${task.progress}%'),
            IconButton(
              onPressed: () => onActionTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.pause,
                color: Colors.green,
              ),
              tooltip: 'Pause',
            ),
            IconButton(
              onPressed: () => onTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.delete,
                color: Colors.red,
              ),
              tooltip: 'Delete',
            )
          ],
        );
      case DownloadTaskStatus.paused:
        return Row(
          children: [
            Text('${task.progress}'),
            IconButton(
              onPressed: () => onActionTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.play_arrow,
                color: Colors.yellow,
              ),
              tooltip: 'Resume',
            ),
            IconButton(
              onPressed: () => onTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.delete,
                color: Colors.red,
              ),
              tooltip: 'Delete',
            )
          ],
        );
      case DownloadTaskStatus.complete:
        return Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            const Text(
              'Ready',
              style: TextStyle(color: Colors.green),
            ),
            IconButton(
              onPressed: () => onActionTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.delete,
                color: Colors.green,
              ),
              tooltip: 'delete',
            )
          ],
        );
      case DownloadTaskStatus.canceled:
        return Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            const Text(
              'Canceled',
              style: TextStyle(color: Colors.red),
            ),
            if (onActionTap != null)
              IconButton(
                onPressed: () => onActionTap?.call(task),
                constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
                icon: const Icon(
                  Icons.delete,
                  color: Colors.red,
                ),
                tooltip: 'Cancel',
              )
          ],
        );

      case DownloadTaskStatus.failed:
        return Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            const Text(
              'Failed',
              style: TextStyle(color: Colors.red),
            ),
            IconButton(
              onPressed: () => onActionTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.delete,
                color: Colors.red,
              ),
              tooltip: 'Delete',
            )
          ],
        );

      case DownloadTaskStatus.enqueued:
        return Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            const Text(
              'Pending',
              style: TextStyle(color: Colors.yellow),
            ),
            IconButton(
              onPressed: () => onActionTap?.call(task),
              constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
              icon: const Icon(
                Icons.delete,
                color: Colors.red,
              ),
              tooltip: 'Delete',
            )
          ],
        );
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: data!.task!.status == DownloadTaskStatus.complete
          ? () {
              onTap!(data!.task);
            }
          : null,
      child: Container(
        padding: const EdgeInsets.only(left: 16, right: 8),
        child: InkWell(
          child: Stack(
            children: [
              SizedBox(
                width: double.infinity,
                height: 64,
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        data!.name!,
                        maxLines: 1,
                        softWrap: true,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(left: 8),
                      child: _buildTrailing(data!.task!),
                    ),
                  ],
                ),
              ),
              if (data!.task!.status == DownloadTaskStatus.running ||
                  data!.task!.status == DownloadTaskStatus.paused)
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 0,
                  child: LinearProgressIndicator(
                    value: data!.task!.progress! / 100,
                  ),
                )
            ],
          ),
        ),
      ),
    );
  }
}
