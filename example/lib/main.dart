import 'package:flutter/material.dart';

import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:m3u8_downloader_example/home_page.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await M3u8Downloader.initialize(debug: true);

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static const _title = "M3u8 Downloader Demo";

  @override
  Widget build(BuildContext context) {
    final platform = Theme.of(context).platform;
    return MaterialApp(
        title: _title,
        theme: ThemeData.light(),
        darkTheme: ThemeData.dark(),
        home: MyHomePage(
          title: _title,
          platform: platform,
        ));
  }
}
