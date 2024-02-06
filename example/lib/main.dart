import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:m3u8_downloader/m3u8_downloader.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _counter = 0;
  final _plugin = M3u8Downloader();

  Future<void> generateRandomNumber() async {
    int result;
    try {
      result = await _plugin.generateRandom();
    } on PlatformException {
      result = 0;
    }

    setState(() {
      _counter = result;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Project Init'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Text('Test Method === Generate Random Number'),
              Text(
                '$_counter',
                style: Theme.of(context).textTheme.headlineLarge,
              )
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: generateRandomNumber,
          tooltip: 'Generate',
          child: const Icon(Icons.refresh),
        ),
      ),
    );
  }
}
