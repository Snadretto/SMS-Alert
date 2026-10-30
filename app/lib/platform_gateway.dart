// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'models.dart';

abstract interface class AlarmGateway {
  Future<AppSnapshot> snapshot();
  Stream<AppSnapshot> get changes;
  Future<void> command(String method, [Map<String, Object?>? arguments]);
  Future<bool> preview(
    AlarmRule rule,
    String sender,
    String body,
    int? simSlot,
  );
}

class AndroidGateway implements AlarmGateway {
  static const _methods = MethodChannel('ch.snadretto.smsalert/methods_v1');
  static const _events = EventChannel('ch.snadretto.smsalert/events_v1');
  @override
  Future<AppSnapshot> snapshot() async => AppSnapshot.fromMap(
    objectMap(await _methods.invokeMethod<Object?>('getSnapshot')),
  );
  @override
  Stream<AppSnapshot> get changes => _events.receiveBroadcastStream().map(
    (event) => AppSnapshot.fromMap(objectMap(event)),
  );
  @override
  Future<void> command(String method, [Map<String, Object?>? arguments]) =>
      _methods.invokeMethod<void>(method, arguments);
  @override
  Future<bool> preview(
    AlarmRule rule,
    String sender,
    String body,
    int? simSlot,
  ) async =>
      await _methods.invokeMethod<bool>('previewRule', {
        'rule': rule.toMap(),
        'sender': sender,
        'body': body,
        'simSlot': simSlot,
      }) ??
      false;
}

class UnsupportedGateway implements AlarmGateway {
  @override
  Future<AppSnapshot> snapshot() async => const AppSnapshot(supported: false);
  @override
  Stream<AppSnapshot> get changes => const Stream.empty();
  @override
  Future<void> command(
    String method, [
    Map<String, Object?>? arguments,
  ]) async => throw UnsupportedError(
    'SMS-Alarmierung ist auf dieser Plattform nicht freigegeben.',
  );
  @override
  Future<bool> preview(
    AlarmRule rule,
    String sender,
    String body,
    int? simSlot,
  ) async => false;
}

AlarmGateway defaultGateway() =>
    !kIsWeb && defaultTargetPlatform == TargetPlatform.android
    ? AndroidGateway()
    : UnsupportedGateway();

class AppController extends ChangeNotifier {
  AppController(this.gateway);
  final AlarmGateway gateway;
  AppSnapshot? data;
  String? error;
  bool busy = false;
  bool _disposed = false;
  StreamSubscription<AppSnapshot>? _subscription;

  Future<void> start() async {
    _subscription = gateway.changes.listen(
      (value) {
        if (_disposed) return;
        data = value;
        notifyListeners();
      },
      onError: (Object failure) {
        if (!_disposed) {
          error = _message(failure);
          notifyListeners();
        }
      },
    );
    await refresh();
  }

  Future<void> refresh() async {
    try {
      final value = await gateway.snapshot();
      if (!_disposed) {
        data = value;
        error = null;
        notifyListeners();
      }
    } catch (failure) {
      if (!_disposed) {
        error = _message(failure);
        notifyListeners();
      }
    }
  }

  Future<bool> run(String method, [Map<String, Object?>? arguments]) async {
    if (busy || _disposed) return false;
    busy = true;
    error = null;
    notifyListeners();
    try {
      await gateway.command(method, arguments);
      await refresh();
      return error == null;
    } catch (failure) {
      if (!_disposed) error = _message(failure);
      return false;
    } finally {
      if (!_disposed) {
        busy = false;
        notifyListeners();
      }
    }
  }

  void dismissError() {
    error = null;
    notifyListeners();
  }

  String _message(Object failure) => switch (failure) {
    PlatformException(:final message) =>
      message ?? 'Die Gerätefunktion konnte nicht ausgeführt werden.',
    FormatException(:final message) => message,
    UnsupportedError() =>
      'Diese Gerätefunktion ist auf dieser Plattform nicht verfügbar.',
    _ => 'Die App konnte den lokalen Gerätestatus nicht laden. Bitte erneut versuchen.',
  };
  @override
  void dispose() {
    _disposed = true;
    unawaited(_subscription?.cancel());
    super.dispose();
  }
}
