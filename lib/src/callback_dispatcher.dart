import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

@pragma('vm:entry-point')
void callbackDispatcher() {
  const backgroundChannel =
      MethodChannel('com.tonybobo.m3u8Downloader_background');

  WidgetsFlutterBinding.ensureInitialized();

  backgroundChannel
    ..setMethodCallHandler((call) async {
      final args = call.arguments as List<dynamic>;
      final handle = CallbackHandle.fromRawHandle(args[0] as int);
      final id = args[1] as String;
      final status = args[2] as int;
      final progress = args[3] as int;

      final callback = PluginUtilities.getCallbackFromHandle(handle) as void
          Function(String id, int status, int progress)?;

      if (callback == null) {
        return;
      }

      callback(id, status, progress);
    })
    ..invokeMethod<void>('didInitializeDispatcher');
}
