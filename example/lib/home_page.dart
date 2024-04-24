import 'dart:io';
import 'dart:isolate';
import 'dart:ui';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:m3u8_downloader_example/data.dart';
import 'package:m3u8_downloader_example/download_list_item.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

class MyHomePage extends StatefulWidget with WidgetsBindingObserver {
  const MyHomePage({super.key, this.platform, required this.title});

  final TargetPlatform? platform;

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<TaskInfo>? _tasks;
  late List<ItemHolder> _items;
  late bool _showContent;
  late bool _permissionReady;
  late bool _saveInPublicStorage;
  final ReceivePort _port = ReceivePort();

  @override
  void initState() {
    super.initState();
    _bindBackgroundIsolate();
    M3u8Downloader.registerCallback(downloadCallback, step: 1);
    initConfig();
    _showContent = false;
    _permissionReady = false;
    _saveInPublicStorage = false;

    _prepare();
  }

  void initConfig() async {
    String? saveDir = await _getDownloadPath();
    M3u8Downloader.config(saveDir: saveDir, connTimeout: 60, readTimeout: 60);
  }

  @override
  void dispose() {
    _unbindBackgroundIsolate();
    super.dispose();
  }

  void _bindBackgroundIsolate() {
    final isSuccess = IsolateNameServer.registerPortWithName(
        _port.sendPort, 'downloader_send_port');
    if (!isSuccess) {
      _unbindBackgroundIsolate();
      _bindBackgroundIsolate();
      return;
    }

    _port.listen((dynamic data) {
      final taskId = (data as List<dynamic>)[0] as String;
      final status = DownloadTaskStatus.fromInt(data[1] as int);
      final progress = data[2] as int;

      // ignore: avoid_print
      print(
        'Callback on UI Isolate: '
        'task ($taskId) is in status ($status) and progress ($progress)',
      );

      if (_tasks != null && _tasks!.isNotEmpty) {
        final task = _tasks!.firstWhere((task) => task.taskId == taskId);
        setState(() {
          task
            ..status = status
            ..progress = progress;
        });
      }
    });
  }

  void _unbindBackgroundIsolate() {
    IsolateNameServer.removePortNameMapping('downloader_send_port');
  }

  @pragma('vm:entry-point')
  static void downloadCallback(String id, int status, int progress) {
    // ignore: avoid_print
    IsolateNameServer.lookupPortByName('downloader_send_port')
        ?.send([id, status, progress]);
  }

  Future<void> _retryRequestPermission() async {
    final hasGranted = await _checkPermission();
    if (hasGranted) {
      await _getDownloadPath();
    }

    setState(() {
      _permissionReady = hasGranted;
    });
  }

  Future<void> _requestDownload(TaskInfo task) async {
    task.taskId =
        await M3u8Downloader.enqueue(url: task.link!, fileName: task.name!);
  }

  Future<void> _pauseDownload(TaskInfo task) async {
    await M3u8Downloader.pause(taskId: task.taskId!);
  }

  Future<void> _resumeDownload(TaskInfo task) async {
    final newTaskId = await M3u8Downloader.resume(taskId: task.taskId!);
    task.taskId = newTaskId;
  }

  // Future<void> _retryDownload(TaskInfo task) async {
  //   final newTaskId = await M3u8Downloader.resume(taskId: task.taskId!);
  //   task.taskId = newTaskId;
  // }

  // Future<bool> _openDownloadedFile(TaskInfo? task) async {
  //   final taskId = task?.taskId;
  //   if (taskId == null) {
  //     return false;
  //   }

  //   return M3u8Downloader.open(taskId: taskId);
  // }

  Future<void> _delete(TaskInfo task) async {
    await M3u8Downloader.remove(taskId: task.taskId!);

    await _prepare();
    setState(() {});
  }

  Future<bool> _checkPermission() async {
    if (Platform.isIOS) return false;

    if (Platform.isAndroid) {
      final info = await DeviceInfoPlugin().androidInfo;
      if (info.version.sdkInt > 28) return true;

      final status = await Permission.storage.status;
      if (status == PermissionStatus.granted) {
        return true;
      }

      final result = await Permission.storage.request();
      return result == PermissionStatus.granted;
    }

    throw StateError('Unknown Platform');
  }

  Future<void> _prepare() async {
    final tasks = await M3u8Downloader.loadTasks();
    final pendingTasks = await M3u8Downloader.loadTasksWithQuery(
        query: "select * from task where status = 1");

    if (tasks == null) {
      // ignore: avoid_print
      print('No tasks found in database');
      return;
    }

    var count = 0;
    _tasks = [];
    _items = [];

    _tasks!.addAll(
        DownloadItems.videos.map((e) => TaskInfo(name: e.name, link: e.url)));

    _items.add(ItemHolder(name: 'Testing Videos'));

    for (var i = count; i < _tasks!.length; i++) {
      _items.add(ItemHolder(name: _tasks![i].name, task: _tasks![i]));
    }

    _items.add(ItemHolder(name: 'Pending Videos'));

    List<TaskInfo?> pendingTasks0 = [];

    pendingTasks0.addAll(
        pendingTasks!.map((e) => TaskInfo(name: e.filename, link: e.url)));

    for (var i = 0; i < pendingTasks.length; i++) {
      _items.add(
          ItemHolder(name: pendingTasks0[i]?.name, task: pendingTasks0[i]));
    }

    for (final task in tasks) {
      for (final info in _tasks!) {
        if (info.link == task.url) {
          info
            ..taskId = task.taskId
            ..status = task.status
            ..progress = task.progress;
        }
      }
    }

    _permissionReady = await _checkPermission();
    if (_permissionReady) {
      await _getDownloadPath();
    }

    setState(() {
      _showContent = true;
    });
  }

  Future<String?> _getDownloadPath() async {
    Directory? directory;
    try {
      if (Platform.isIOS) {
        directory = await getApplicationDocumentsDirectory();
      } else {
        directory = Directory('/storage/emulated/0/Download');
        if (!directory.existsSync()) await directory.create();
      }
    } catch (err) {
      print("download folder error : $err");
    }
    print("directory : $directory");
    return directory!.path;
  }

  Widget _buildDownloadList() {
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 16),
      children: [
        Row(
          children: [
            Checkbox(
              value: _saveInPublicStorage,
              onChanged: (newValue) {
                setState(
                  () {
                    _saveInPublicStorage = newValue ?? false;
                  },
                );
              },
            ),
            const Text('Save in Public Storage'),
          ],
        ),
        ..._items.map(
          (item) {
            final task = item.task;
            if (task == null) {
              return Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Text(
                  item.name!,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Colors.blue,
                    fontSize: 18,
                  ),
                ),
              );
            }
            return DownloadListItem(
              data: item,
              onTap: (task) async {
                _delete(task!);

                // final success = await _openDownloadedFile(task);
                // if (!success) {
                //   ScaffoldMessenger.of(context).showSnackBar(
                //       const SnackBar(content: Text('Cannot Open This file')));
                // }
              },
              onActionTap: (task) {
                switch (task.status) {
                  case DownloadTaskStatus.undefined:
                    _requestDownload(task);

                  case DownloadTaskStatus.running:
                    _pauseDownload(task);

                  case DownloadTaskStatus.paused:
                    _resumeDownload(task);
                  case DownloadTaskStatus.complete:
                    _delete(task);

                  case DownloadTaskStatus.canceled:
                  // _delete(task);

                  case DownloadTaskStatus.failed:
                    // _retryDownload(task);
                    _delete(task);
                  case DownloadTaskStatus.enqueued:
                    _delete(task);
                  default:
                    return;
                }
              },
              // onCancel: _delete,
            );
          },
        )
      ],
    );
  }

  Widget _buildNoPermissionWarning() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 24),
            child: Text(
              'Grant Storage Permission to continue',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.blueGrey, fontSize: 18),
            ),
          ),
          const SizedBox(height: 32),
          TextButton(
            onPressed: _retryRequestPermission,
            child: const Text(
              'Retry',
              style: TextStyle(
                  color: Colors.blue,
                  fontWeight: FontWeight.bold,
                  fontSize: 20),
            ),
          )
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          if (Platform.isIOS)
            PopupMenuButton<Function>(
              icon: const Icon(
                Icons.more_vert,
                color: Colors.white,
              ),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8)),
              itemBuilder: (context) => [
                PopupMenuItem(
                  onTap: () => exit(0),
                  child: const ListTile(
                    title: Text(
                      'Simulate App Backgrounded',
                      style: TextStyle(fontSize: 15),
                    ),
                  ),
                ),
              ],
            ),
        ],
      ),
      body: Builder(
        builder: (context) {
          if (!_showContent) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          return _permissionReady
              ? _buildDownloadList()
              : _buildNoPermissionWarning();
        },
      ),
    );
  }
}
