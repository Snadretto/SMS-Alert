// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'platform_gateway.dart';
import 'screens.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  LicenseRegistry.addLicense(() async* {
    yield LicenseEntryWithLineBreaks(
      [
        'SMS-Alert',
        'Google libphonenumber',
        'Kotlin standard library',
        'AndroidX',
      ],
      '${await rootBundle.loadString('assets/NATIVE_NOTICES')}\n\n${await rootBundle.loadString('assets/NOTICE')}\n\n${await rootBundle.loadString('assets/LICENSE')}',
    );
  });
  runApp(const SmsAlertApp());
}

class SmsAlertApp extends StatefulWidget {
  const SmsAlertApp({super.key, this.gateway});
  final AlarmGateway? gateway;
  @override
  State<SmsAlertApp> createState() => _SmsAlertAppState();
}

class _SmsAlertAppState extends State<SmsAlertApp> with WidgetsBindingObserver {
  late final AppController controller;
  @override
  void initState() {
    super.initState();
    controller = AppController(widget.gateway ?? defaultGateway());
    WidgetsBinding.instance.addObserver(this);
    unawaited(controller.start());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) unawaited(controller.refresh());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    controller.dispose();
    super.dispose();
  }

  ThemeData theme(Brightness brightness) => ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF006B5E),
      brightness: brightness,
    ),
    inputDecorationTheme: const InputDecorationTheme(
      border: OutlineInputBorder(),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(minimumSize: const Size(48, 48)),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(minimumSize: const Size(48, 48)),
    ),
  );
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'SMS-Alert',
    debugShowCheckedModeBanner: false,
    theme: theme(Brightness.light),
    darkTheme: theme(Brightness.dark),
    locale: const Locale('de'),
    supportedLocales: const [Locale('de')],
    localizationsDelegates: GlobalMaterialLocalizations.delegates,
    home: AnimatedBuilder(
      animation: controller,
      builder: (context, _) => AppShell(controller: controller),
    ),
  );
}
