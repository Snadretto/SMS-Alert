// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sms_alert/main.dart';
import 'package:sms_alert/models.dart';
import 'package:sms_alert/platform_gateway.dart';

class FakeGateway implements AlarmGateway {
  FakeGateway(this.value);
  AppSnapshot value;
  final calls = <(String, Map<String, Object?>?)>[];
  final updates = StreamController<AppSnapshot>.broadcast();
  Object? failure;
  @override
  Future<AppSnapshot> snapshot() async => value;
  @override
  Stream<AppSnapshot> get changes => updates.stream;
  @override
  Future<void> command(String method, [Map<String, Object?>? arguments]) async {
    if (failure case final Object error) throw error;
    calls.add((method, arguments));
  }

  @override
  Future<bool> preview(
    AlarmRule rule,
    String sender,
    String body,
    int? simSlot,
  ) async => true;
}

AppSnapshot state({
  bool enabled = false,
  List<AlarmEvent> pending = const [],
}) => AppSnapshot(
  supported: true,
  settings: AlarmSettings(enabled: enabled),
  rules: const [AlarmRule(id: 'r', name: 'Pikett', text: 'Alarm')],
  pending: pending,
  pendingCount: pending.length,
  diagnostics: const {
    'api': 37,
    'smsPermission': true,
    'notificationsEnabled': true,
    'notificationPermission': true,
    'alarmAudioAccess': true,
    'alarmVolume': 5,
  },
);
const event = AlarmEvent(
  id: 'event-1',
  receivedAt: 1793361600000,
  source: 'SMS',
  ruleNames: ['Pikett'],
  status: 'PENDING',
  senderLabel: '••••4567',
);

void main() {
  test('unknown platform contract version is rejected', () {
    expect(
      () => AppSnapshot.fromMap({'schemaVersion': 99}),
      throwsFormatException,
    );
    expect(
      AppSnapshot.fromMap({'schemaVersion': 1, 'supported': false}).supported,
      isFalse,
    );
  });
  test('Android 17 readiness requires alarm audio access; optional fullscreen does not block', () {
    expect(state().configured, isTrue);
    final base = state();
    expect(
      AppSnapshot(
        supported: true,
        rules: base.rules,
        diagnostics: {...base.diagnostics, 'alarmAudioAccess': false},
      ).configured,
      isFalse,
    );
    expect(const AppSnapshot(supported: false).configured, isFalse);
  });
  test(
    'controller reports permission denial and accepts later updates',
    () async {
      final gateway = FakeGateway(state())
        ..failure = PlatformException(
          code: 'denied',
          message: 'Zugriff verweigert',
        );
      final controller = AppController(gateway);
      await controller.start();
      expect(await controller.run('requestCorePermissions'), isFalse);
      expect(controller.error, 'Zugriff verweigert');
      expect(controller.busy, isFalse);
      gateway.failure = null;
      gateway.value = state(enabled: true);
      expect(await controller.run('setEnabled', {'enabled': true}), isTrue);
      expect(controller.data!.settings.enabled, isTrue);
      controller.dispose();
      await gateway.updates.close();
    },
  );
  testWidgets(
    'unsupported platform gives explicit limitation and no activation',
    (tester) async {
      await tester.pumpWidget(SmsAlertApp(gateway: UnsupportedGateway()));
      await tester.pumpAndSettle();
      expect(find.text('SMS-Alarmierung hier nicht verfügbar'), findsOneWidget);
      expect(find.text('Alarmierung aktivieren'), findsNothing);
    },
  );
  testWidgets('pause needs confirmation and cancellation keeps app enabled', (
    tester,
  ) async {
    final gateway = FakeGateway(state(enabled: true, pending: [event]));
    await tester.pumpWidget(SmsAlertApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('App pausieren'));
    await tester.pumpAndSettle();
    expect(gateway.calls, isEmpty);
    await tester.tap(find.text('Zurück'));
    await tester.pumpAndSettle();
    expect(gateway.calls, isEmpty);
    await tester.tap(find.text('App pausieren'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Pausieren'));
    await tester.pumpAndSettle();
    expect(gateway.calls.single.$1, 'setEnabled');
    expect(gateway.calls.single.$2, {'enabled': false});
  });
  testWidgets('acknowledgement carries only selected event ID', (tester) async {
    final gateway = FakeGateway(state(enabled: true, pending: [event]));
    await tester.pumpWidget(SmsAlertApp(gateway: gateway));
    await tester.pumpAndSettle();
    final button = find.text('Diesen Alarm quittieren');
    await tester.ensureVisible(button);
    await tester.tap(button);
    await tester.pumpAndSettle();
    expect(gateway.calls.single.$1, 'acknowledge');
    expect(gateway.calls.single.$2, {'id': 'event-1'});
  });
  testWidgets('rule editor validates criteria before native save', (
    tester,
  ) async {
    final gateway = FakeGateway(const AppSnapshot(supported: true));
    await tester.pumpWidget(SmsAlertApp(gateway: gateway));
    await tester.pumpAndSettle();
    final add = find.text('Erste Regel anlegen');
    await tester.ensureVisible(add);
    await tester.tap(add);
    await tester.pumpAndSettle();
    final save = find.text('Regel speichern');
    FocusManager.instance.primaryFocus?.unfocus();
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      save,
      400,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(save);
    await tester.pumpAndSettle();
    await tester.tap(save);
    await tester.pumpAndSettle();
    expect(gateway.calls, isEmpty);
    final name = find.widgetWithText(TextFormField, 'Regelname');
    await tester.scrollUntilVisible(
      name,
      -400,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.enterText(name, 'Pikett');
    final text = find.widgetWithText(TextFormField, 'Text enthält');
    await tester.scrollUntilVisible(
      text,
      200,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.enterText(text, 'ALARM');
    FocusManager.instance.primaryFocus?.unfocus();
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      save,
      400,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(save);
    await tester.pumpAndSettle();
    await tester.tap(save);
    await tester.pumpAndSettle();
    expect(gateway.calls.single.$1, 'saveRule');
    expect(gateway.calls.single.$2!['text'], 'ALARM');
  });
  testWidgets('small screen with large text can scroll without overflowing', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.7;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
      tester.platformDispatcher.clearTextScaleFactorTestValue();
    });
    await tester.pumpWidget(
      SmsAlertApp(
        gateway: FakeGateway(state(enabled: true, pending: [event, event])),
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('Einrichtung'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
}
