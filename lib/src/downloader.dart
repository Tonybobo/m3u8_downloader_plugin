import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:m3u8_downloader/src/exceptions.dart';
import 'package:m3u8_downloader/src/models.dart';

import 'callback_dispatcher.dart';

typedef DownloadCallback = void Function(String id, int status, int progress);

class M3u8Downloader {
  static const _channel = MethodChannel("com.tonybobo.m3u8_downloader");

  static bool _initialized = false;

  static bool get initialized => _initialized;

  static bool _debug = false;

  static bool get debug => _debug;

  static Future<void> initialize(
      {bool debug = false, bool ignoreSSL = false}) async {
    assert(!_initialized, "M3U8 Downloader has been initialized");

    _debug = debug;

    final callback = PluginUtilities.getCallbackHandle(callbackDispatcher);

    await _channel.invokeMethod<void>('initialize', <dynamic>[
      callback!.toRawHandle(),
      if (debug) 1 else 0,
      if (ignoreSSL) 1 else 0
    ]);

    _initialized = true;
  }

  static Future<bool> config({
    String? saveDir,
    int? connTimeout,
    int? readTimeout,
  }) async {
    final bool? result = await _channel.invokeMethod<bool>('config', {
      "saveDir": saveDir,
      "connTimeout": connTimeout,
      "readTimeout": readTimeout
    });
    return result ?? false;
  }

  static Future<String?> enqueue(
      {required String url,
      required String fileName,
      int timeout = 15000}) async {
    assert(_initialized, "M3U8 Downloader is not initialized.");

    try {
      final taskId = await _channel
          .invokeMethod<String>('enqueue', {'url': url, 'filename': fileName});

      if (taskId == null) {
        throw const M3U8DownloaderException(
            message: 'Enqueue Operation return null taskID');
      }

      return taskId;
    } on M3U8DownloaderException catch (e) {
      _log('Failed to enqueue task. ${e.message}');
    } on PlatformException catch (e) {
      _log('Failed to enqueue task ${e.message}');
    }
    return null;
  }

  static Future<List<DownloadTask>?> loadTasks() async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    try {
      final result = await _channel.invokeMethod<List<dynamic>>("loadTasks");
      if (result == null) {
        throw const M3U8DownloaderException(
            message: 'Load Tasks Operation return null');
      }

      return result.map(
        (dynamic item) {
          return DownloadTask(
            taskId: item['task_id'] as String,
            status: DownloadTaskStatus.fromInt(item['status'] as int),
            progress: item['progress'] as int,
            url: item['url'] as String,
            filename: item['file_name'] as String,
            timeCreated: item['time_created'] as int,
          );
        },
      ).toList();
    } on M3U8DownloaderException catch (e) {
      _log("Fail to Load tasks. ${e.message}");
    } on PlatformException catch (e) {
      _log(e.message);
    }

    return null;
  }

  static Future<List<DownloadTask>?> loadTasksWithQuery(
      {required String query}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    try {
      final result = await _channel.invokeMethod<List<dynamic>>(
        'loadTasksWithRawQuery',
        {'query': query},
      );

      if (result == null) {
        throw const M3U8DownloaderException(
            message: 'Load Tasks with Query Operation return null');
      }

      return result.map(
        (dynamic item) {
          return DownloadTask(
            taskId: item['task_id'] as String,
            status: DownloadTaskStatus.fromInt(item['status'] as int),
            progress: item['progress'] as int,
            url: item['url'] as String,
            filename: item['file_name'] as String,
            timeCreated: item['time_created'] as int,
          );
        },
      ).toList();
    } on M3U8DownloaderException catch (e) {
      _log("Fail to LoadTasksWithQuery ${e.message}");
    } on PlatformException catch (e) {
      _log(e.message);
    }

    return null;
  }

  static Future<void> cancel({required String taskId}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");
    try {
      return await _channel.invokeMethod<void>('cancel', {'task_id': taskId});
    } on PlatformException catch (e) {
      _log(e.message);
    }
  }

  static Future<void> cancelAll() async {
    assert(_initialized, "M3U8 Downloader has not been initialized");
    try {
      return await _channel.invokeMethod<void>('cancelAll');
    } on PlatformException catch (e) {
      _log(e.message);
    }
  }

  static Future<void> pause({required String taskId}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");
    try {
      return await _channel.invokeMethod<void>('pause', {'task_id': taskId});
    } on PlatformException catch (e) {
      _log(e.message);
    }
  }

  static Future<String?> resume(
      {required String taskId,
      bool requiresStorageNotLow = true,
      int timeout = 15000}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    try {
      return await _channel.invokeMethod('resume', {
        'task_id': taskId,
        'requires_storage_not_low': requiresStorageNotLow,
        'timeout': timeout
      });
    } on PlatformException catch (e) {
      _log(e.message);
      return null;
    }
  }

  static Future<String?> retry(
      {required String taskId,
      bool requiresStorageNotLow = true,
      int timeout = 15000}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    try {
      return await _channel.invokeMethod('retry', {
        'task_id': taskId,
        'requires_storage_not_low': requiresStorageNotLow,
        'timeout': timeout
      });
    } on PlatformException catch (e) {
      _log(e.message);
      return null;
    }
  }

  static Future<void> remove({
    required String taskId,
    bool shouldDeleteContent = true,
  }) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    try {
      return await _channel.invokeMethod('remove', {
        'task_id': taskId,
        'should_delete_content': shouldDeleteContent,
      });
    } on PlatformException catch (e) {
      _log(e.message);
    }
  }

  static Future<bool> open({required String taskId}) async {
    assert(_initialized, "M3U8 Downloader has not been initialized");

    bool? result;
    try {
      result = await _channel.invokeMethod('open', {
        'task_id': taskId,
      });

      if (result == null) {
        throw const M3U8DownloaderException(
            message: 'Open operation return null');
      }
    } on PlatformException catch (e) {
      _log('Fail to Open Downloaded file. Reason: ${e.message}');
    }

    return result ?? false;
  }

  static Future<void> registerCallback(DownloadCallback callback,
      {int step = 10}) async {
    assert(_initialized, "M3U8 Download has not been initialized");

    final callbackHandle = PluginUtilities.getCallbackHandle(callback);

    assert(callbackHandle != null,
        "Callback must be a top-level or static function");

    assert(0 <= step && step <= 100,
        "step size is not in the inclusive <0,100> range");

    await _channel.invokeMethod<void>(
        'registerCallback', <dynamic>[callbackHandle!.toRawHandle(), step]);
  }

  static void _log(String? message) {
    if (_debug) {
      // ignore: avoid_print
      print(message);
    }
  }
}
