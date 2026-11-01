// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'package:flutter/foundation.dart';

Map<String, Object?> objectMap(Object? value) {
  if (value is! Map) {
    throw const FormatException('Ungültige Antwort der Geräteanbindung.');
  }
  return Map<String, Object?>.from(value);
}

List<Map<String, Object?>> objectList(Object? value) {
  if (value is! List) {
    throw const FormatException('Ungültige Liste der Geräteanbindung.');
  }
  return value.map(objectMap).toList(growable: false);
}

@immutable
class AlarmRule {
  const AlarmRule({
    this.id = '',
    required this.name,
    this.sender = '',
    this.text = '',
    this.logic = 'AND',
    this.enabled = true,
    this.caseSensitive = false,
    this.simSlot,
  });
  final String id, name, sender, text, logic;
  final bool enabled, caseSensitive;
  final int? simSlot;
  factory AlarmRule.fromMap(Map<String, Object?> map) => AlarmRule(
    id: map['id']! as String,
    name: map['name']! as String,
    sender: map['sender']! as String,
    text: map['text']! as String,
    logic: map['logic']! as String,
    enabled: map['enabled']! as bool,
    caseSensitive: map['caseSensitive']! as bool,
    simSlot: map['simSlot'] as int?,
  );
  Map<String, Object?> toMap() => {
    'id': id,
    'name': name,
    'sender': sender,
    'text': text,
    'logic': logic,
    'enabled': enabled,
    'caseSensitive': caseSensitive,
    'simSlot': simSlot,
  };
  AlarmRule withEnabled(bool value) => AlarmRule(
    id: id,
    name: name,
    sender: sender,
    text: text,
    logic: logic,
    enabled: value,
    caseSensitive: caseSensitive,
    simSlot: simSlot,
  );
  String get summary {
    final parts = [
      if (sender.isNotEmpty) 'Von $sender',
      if (text.isNotEmpty) 'Text enthält „$text“',
    ];
    return parts.join(logic == 'AND' ? ' UND ' : ' ODER ');
  }
}

@immutable
class DeviceTone {
  const DeviceTone({required this.id, required this.title, required this.uri});
  final String id, title, uri;
  factory DeviceTone.fromMap(Map<String, Object?> map) => DeviceTone(
    id: map['id']! as String,
    title: map['title']! as String,
    uri: map['uri']! as String,
  );
  Map<String, Object?> toMap() => {'id': id, 'title': title, 'uri': uri};
}

@immutable
class AlarmSettings {
  const AlarmSettings({
    this.enabled = false,
    this.country = 'CH',
    this.volume = 80,
    this.vibration = true,
    this.tone = 'beacon',
    this.raiseSystemVolume = false,
    this.retentionDays = 30,
    this.historyLimit = 200,
    this.deviceTone,
  });
  final bool enabled, vibration, raiseSystemVolume;
  final String country, tone;
  final int volume, retentionDays, historyLimit;
  final DeviceTone? deviceTone;
  factory AlarmSettings.fromMap(Map<String, Object?> map) => AlarmSettings(
    enabled: map['enabled']! as bool,
    country: map['country']! as String,
    volume: map['volume']! as int,
    vibration: map['vibration']! as bool,
    tone: map['tone']! as String,
    raiseSystemVolume: map['raiseSystemVolume']! as bool,
    retentionDays: map['retentionDays']! as int,
    historyLimit: map['historyLimit']! as int,
    deviceTone: map['deviceTone'] == null
        ? null
        : DeviceTone.fromMap(objectMap(map['deviceTone'])),
  );
  Map<String, Object?> toMap() => {
    'country': country,
    'volume': volume,
    'vibration': vibration,
    'tone': tone,
    'raiseSystemVolume': raiseSystemVolume,
    'retentionDays': retentionDays,
    'historyLimit': historyLimit,
    'deviceTone': deviceTone?.toMap(),
  };
  AlarmSettings copyWith({
    String? country,
    int? volume,
    bool? vibration,
    String? tone,
    bool? raiseSystemVolume,
    int? retentionDays,
    int? historyLimit,
    DeviceTone? deviceTone,
  }) => AlarmSettings(
    enabled: enabled,
    country: country ?? this.country,
    volume: volume ?? this.volume,
    vibration: vibration ?? this.vibration,
    tone: tone ?? this.tone,
    raiseSystemVolume: raiseSystemVolume ?? this.raiseSystemVolume,
    retentionDays: retentionDays ?? this.retentionDays,
    historyLimit: historyLimit ?? this.historyLimit,
    deviceTone: deviceTone ?? this.deviceTone,
  );
}

@immutable
class AlarmEvent {
  const AlarmEvent({
    required this.id,
    required this.receivedAt,
    required this.source,
    required this.ruleNames,
    required this.status,
    required this.senderLabel,
    this.simSlot,
    this.resolvedAt,
    this.startedAt,
    this.latencyMs,
    this.lastOutcome,
  });
  final String id, source, status, senderLabel;
  final int receivedAt;
  final int? simSlot, resolvedAt;
  final int? startedAt, latencyMs;
  final String? lastOutcome;
  final List<String> ruleNames;
  factory AlarmEvent.fromMap(Map<String, Object?> map) => AlarmEvent(
    id: map['id']! as String,
    receivedAt: map['receivedAt']! as int,
    source: map['source']! as String,
    status: map['status']! as String,
    ruleNames: (map['ruleNames']! as List).cast<String>(),
    senderLabel: map['senderLabel']! as String,
    simSlot: map['simSlot'] as int?,
    resolvedAt: map['resolvedAt'] as int?,
    startedAt: map['startedAt'] as int?,
    latencyMs: map['latencyMs'] as int?,
    lastOutcome: map['lastOutcome'] as String?,
  );
  bool get isTest => source == 'TEST';
  String get title => isTest ? 'Lokaler Testalarm' : ruleNames.join(', ');
  String get statusLabel => switch (status) {
    'PENDING' => 'Offen',
    'ACKNOWLEDGED' => 'Quittiert',
    'PAUSED' => 'Durch App-Pause beendet',
    _ => 'Unbekannter Zustand',
  };
}

@immutable
class SetupCheck {
  const SetupCheck(
    this.title,
    this.detail,
    this.ok,
    this.action, {
    this.required = true,
  });
  final String title, detail, action;
  final bool ok, required;
}

@immutable
class AppSnapshot {
  const AppSnapshot({
    required this.supported,
    this.settings = const AlarmSettings(),
    this.rules = const [],
    this.pending = const [],
    this.pendingCount = 0,
    this.history = const [],
    this.diagnostics = const {},
  });
  final bool supported;
  final AlarmSettings settings;
  final List<AlarmRule> rules;
  final List<AlarmEvent> pending, history;
  final int pendingCount;
  final Map<String, Object?> diagnostics;
  factory AppSnapshot.fromMap(Map<String, Object?> map) {
    if (map['schemaVersion'] != 1) {
      throw const FormatException(
        'Die Geräteanbindung hat eine unbekannte Version.',
      );
    }
    if (map['supported'] != true) return const AppSnapshot(supported: false);
    return AppSnapshot(
      supported: true,
      settings: AlarmSettings.fromMap(objectMap(map['settings'])),
      rules: objectList(map['rules'])
          .map(AlarmRule.fromMap)
          .toList(growable: false),
      pending: objectList(map['pending'])
          .map(AlarmEvent.fromMap)
          .toList(growable: false),
      pendingCount: map['pendingCount']! as int,
      history: objectList(map['history'])
          .map(AlarmEvent.fromMap)
          .toList(growable: false),
      diagnostics: objectMap(map['diagnostics']),
    );
  }
  bool flag(String key) => diagnostics[key] == true;
  int number(String key, [int fallback = 0]) =>
      diagnostics[key] as int? ?? fallback;
  int get activeRules => rules.where((rule) => rule.enabled).length;
  List<SetupCheck> get checks => [
    SetupCheck(
      'SMS empfangen',
      flag('smsPermission')
          ? 'SMS-Empfang ist freigegeben. Kein Zugriff auf das Postfach oder Kontakte.'
          : 'SMS-Empfang ist noch nicht freigegeben. Meldet Android „App wurde Zugriff verweigert“ oder „eingeschränkte Einstellungen“, öffne die hervorgehobene Hilfe unter dieser Karte.',
      flag('smsPermission'),
      'smsPermission',
    ),
    SetupCheck(
      'Alarmbenachrichtigungen',
      'Quittierung und Status müssen erreichbar bleiben.',
      flag('notificationsEnabled') && flag('notificationPermission'),
      'notifications',
    ),
    if (number('api') >= 37)
      SetupCheck(
        'Alarm-Audio im Hintergrund',
        'Android 17: Unter „Wecker und Erinnerungen“ freigeben. Die App plant dafür keine zeitgesteuerten Alarme.',
        flag('alarmAudioAccess'),
        'alarmAccess',
      ),
    SetupCheck(
      'System-Alarmlautstärke',
      'Lautlos ist nicht dasselbe wie Alarmlautstärke null. Bei null bleibt Ton möglicherweise stumm.',
      number('alarmVolume') > 0 || settings.raiseSystemVolume,
      'sound',
    ),
    SetupCheck(
      'Nicht stören',
      'Alarme im gewünschten DND-Profil erlauben und auf diesem Gerät testen. Der Zugriff allein garantiert keinen Ton.',
      flag('dndAccess'),
      'dnd',
      required: false,
    ),
    SetupCheck(
      'Alarmkanal',
      'Benachrichtigungen und gegebenenfalls die DND-Ausnahme des Kanals prüfen.',
      flag('notificationsEnabled'),
      'channel',
      required: false,
    ),
    SetupCheck(
      'Vollbildalarm',
      'Optional: Ohne diese Freigabe bleiben Benachrichtigung und native Alarmsteuerung aktiv.',
      flag('fullScreenAllowed'),
      'fullscreen',
      required: false,
    ),
    SetupCheck(
      'Energieverwaltung',
      'Optional bei Herstellerbeschränkungen: Einstellungen prüfen; keine pauschale Freigabe aller Apps nötig.',
      flag('batteryExempt'),
      'battery',
      required: false,
    ),
  ];
  bool get configured =>
      supported &&
      activeRules > 0 &&
      checks.where((check) => check.required).every((check) => check.ok);
}

String outcomeLabel(String code) => switch (code) {
  'not_tested' => 'Noch kein Alarm getestet',
  'alarm_queued' => 'Alarm wartet auf Start',
  'playback_started' => 'Tonwiedergabe gestartet – Hörbarkeit am Gerät prüfen',
  'acknowledged' => 'Alarm quittiert',
  'paused' => 'App bewusst pausiert',
  'alarm_audio_access_required' => 'Alarm-Audio-Zugriff fehlt',
  'notifications_blocked' => 'Alarmbenachrichtigungen sind blockiert',
  'audio_focus_lost' =>
    'System hat Ton unterbrochen; Quittierung bleibt verfügbar',
  'audio_focus_denied' => 'System hat Audiofokus verweigert',
  'audio_failed' => 'Ton konnte nicht gestartet werden',
  'background_start_blocked' => 'System hat den Hintergrundstart blockiert',
  'alarm_interrupted' => 'Offener Alarm wurde unterbrochen – Fortsetzen prüfen',
  'device_restarted' => 'Gerät neu gestartet – Einrichtung prüfen',
  'app_updated' => 'App aktualisiert; Regeln und Pause wurden beibehalten',
  'sms_processing_failed' => 'SMS-Ereignis konnte nicht verarbeitet werden',
  _ => 'Status aktualisiert',
};
