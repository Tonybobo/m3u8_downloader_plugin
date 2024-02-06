import 'package:flutter/services.dart';

class M3u8Downloader {
  static const _channel = MethodChannel("com.tonybobo.m3u8_downloader");

  Future<int> generateRandom() async {
    int random;
    try {
      random = await _channel.invokeMethod('getRandomNumber');
    } on PlatformException {
      random = 0;
    }
    return random;
  }
}
