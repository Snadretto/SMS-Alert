// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'package:flutter/material.dart';

import 'models.dart';
import 'platform_gateway.dart';
import 'rule_editor.dart';
import 'settings_screen.dart';

String eventTime(BuildContext context, int milliseconds) {
  final date = DateTime.fromMillisecondsSinceEpoch(milliseconds);
  final local = MaterialLocalizations.of(context);
  return '${local.formatMediumDate(date)} · ${local.formatTimeOfDay(TimeOfDay.fromDateTime(date), alwaysUse24HourFormat: true)}';
}

Future<bool> confirm(
  BuildContext context,
  String title,
  String message,
  String action,
) async =>
    await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Zurück'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(action),
          ),
        ],
      ),
    ) ??
    false;

class SectionTitle extends StatelessWidget {
  const SectionTitle(this.title, {super.key, this.trailing});
  final String title;
  final Widget? trailing;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 24, bottom: 12),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleLarge),
        if (trailing != null)
          Align(alignment: Alignment.centerRight, child: trailing!),
      ],
    ),
  );
}

class NoteCard extends StatelessWidget {
  const NoteCard({
    super.key,
    required this.title,
    required this.text,
    this.icon = Icons.info_outline,
    this.action,
  });
  final String title, text;
  final IconData icon;
  final Widget? action;
  @override
  Widget build(BuildContext context) => Card(
    elevation: 0,
    margin: EdgeInsets.zero,
    color: Theme.of(context).colorScheme.surfaceContainerLow,
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(text),
          if (action != null) ...[const SizedBox(height: 12), action!],
        ],
      ),
    ),
  );
}

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.controller});
  final AppController controller;
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int selected = 0;
  AppController get controller => widget.controller;
  void select(int index) => setState(() => selected = index);
  Future<void> activateOrPause(AppSnapshot snapshot) async {
    if (snapshot.settings.enabled) {
      if (!await confirm(
        context,
        'Alarmierung pausieren?',
        'Alle offenen Alarme werden beendet. Neue SMS lösen bis zur manuellen Reaktivierung keinen Alarm aus.',
        'Pausieren',
      )) {
        return;
      }
    } else if (!snapshot.configured && snapshot.activeRules > 0) {
      if (!await confirm(
        context,
        'Einrichtung noch eingeschränkt',
        'Einige Systemeinstellungen fehlen. Aktivieren allein garantiert noch keinen hörbaren Alarm. Einrichtung anschließend prüfen.',
        'Trotzdem aktivieren',
      )) {
        return;
      }
    }
    await controller.run('setEnabled', {'enabled': !snapshot.settings.enabled});
  }

  Future<void> editRule([AlarmRule? rule]) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => RuleEditor(controller: controller, rule: rule),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final data = controller.data;
    return Scaffold(
      appBar: AppBar(
        title: const Text('SMS-Alert'),
        actions: [
          IconButton(
            tooltip: 'Aktualisieren',
            onPressed: controller.busy ? null : controller.refresh,
            icon: const Icon(Icons.refresh),
          ),
          if (data?.supported == true)
            IconButton(
              tooltip: 'Einstellungen',
              onPressed: () => Navigator.of(context).push<void>(
                MaterialPageRoute(
                  builder: (_) => SettingsScreen(controller: controller),
                ),
              ),
              icon: const Icon(Icons.settings_outlined),
            ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (controller.busy) const LinearProgressIndicator(),
            if (controller.error != null)
              MaterialBanner(
                forceActionsBelow: true,
                content: Text(controller.error!),
                leading: const Icon(Icons.error_outline),
                actions: [
                  TextButton(
                    onPressed: controller.refresh,
                    child: const Text('Erneut versuchen'),
                  ),
                  TextButton(
                    onPressed: controller.dismissError,
                    child: const Text('Schließen'),
                  ),
                ],
              ),
            if (data != null && data.pendingCount > 0 && selected != 0)
              MaterialBanner(
                forceActionsBelow: true,
                content: Text('${data.pendingCount} offene Alarme'),
                leading: const Icon(Icons.notification_important_outlined),
                actions: [
                  TextButton(
                    onPressed: () => select(0),
                    child: const Text('Zu den Alarmen'),
                  ),
                ],
              ),
            Expanded(
              child: data == null
                  ? Center(
                      child: controller.error == null
                          ? const CircularProgressIndicator()
                          : const Text(
                              'Gerätestatus ist noch nicht verfügbar.',
                            ),
                    )
                  : !data.supported
                  ? const _UnsupportedScreen()
                  : switch (selected) {
                      0 => home(data),
                      1 => rules(data),
                      2 => history(data),
                      _ => setup(data),
                    },
            ),
          ],
        ),
      ),
      bottomNavigationBar: data?.supported != true
          ? null
          : NavigationBar(
              selectedIndex: selected,
              onDestinationSelected: select,
              destinations: const [
                NavigationDestination(
                  icon: Icon(Icons.shield_outlined),
                  selectedIcon: Icon(Icons.shield),
                  label: 'Übersicht',
                ),
                NavigationDestination(
                  icon: Icon(Icons.tune_outlined),
                  selectedIcon: Icon(Icons.tune),
                  label: 'Regeln',
                ),
                NavigationDestination(
                  icon: Icon(Icons.history),
                  label: 'Verlauf',
                ),
                NavigationDestination(
                  icon: Icon(Icons.fact_check_outlined),
                  selectedIcon: Icon(Icons.fact_check),
                  label: 'Einrichtung',
                ),
              ],
            ),
    );
  }

  Widget page(List<Widget> children) => SingleChildScrollView(
    padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
    child: Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 760),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: children,
        ),
      ),
    ),
  );
  Widget home(AppSnapshot data) {
    final colors = Theme.of(context).colorScheme;
    final active = data.settings.enabled;
    return page([
      Text(
        'Dein Pikett. Dein Alarm.',
        style: Theme.of(context).textTheme.headlineSmall,
      ),
      const SizedBox(height: 8),
      const Text(
        'SMS erkennen und auf diesem Gerät alarmieren. Ohne Konto und ohne Cloud.',
      ),
      const SizedBox(height: 24),
      Card(
        elevation: 0,
        margin: EdgeInsets.zero,
        color: active
            ? colors.primaryContainer
            : colors.surfaceContainerHighest,
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    active ? Icons.shield : Icons.pause_circle_outline,
                    size: 36,
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Text(
                      active ? 'Alarmierung aktiviert' : 'Alarmierung pausiert',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                active
                    ? '${data.activeRules} aktive Regeln · ${data.configured ? 'technisch eingerichtet' : 'Einrichtung prüfen'}'
                    : 'Neue SMS bleiben ohne Alarm, bis du die App wieder aktivierst.',
              ),
              const SizedBox(height: 20),
              Semantics(
                identifier: 'toggle_activation',
                child: FilledButton.icon(
                  onPressed: controller.busy
                      ? null
                      : () => activateOrPause(data),
                  icon: Icon(active ? Icons.pause : Icons.play_arrow),
                  label: Text(
                    active ? 'App pausieren' : 'Alarmierung aktivieren',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      if (data.activeRules == 0) ...[
        const SizedBox(height: 16),
        NoteCard(
          title: 'Mit einer Regel beginnen',
          text: 'Lege fest, welche Absender oder Nachrichtentexte dich alarmieren sollen.',
          icon: Icons.add_alert_outlined,
          action: OutlinedButton.icon(
            onPressed: editRule,
            icon: const Icon(Icons.add),
            label: const Text('Erste Regel anlegen'),
          ),
        ),
      ],
      if (!data.configured && data.activeRules > 0) ...[
        const SizedBox(height: 16),
        NoteCard(
          title: 'Einrichtung kontrollieren',
          text: 'SMS-Empfang, Benachrichtigungen und Alarm-Audio benötigen passende Systemfreigaben.',
          action: TextButton(
            onPressed: () => select(3),
            child: const Text('Einrichtung öffnen'),
          ),
        ),
      ],
      if (data.pendingCount > 0) ...[
        SectionTitle(
          '${data.pendingCount} offene Alarme',
          trailing: data.pendingCount > 1
              ? TextButton(
                  onPressed: controller.busy
                      ? null
                      : () async {
                          if (await confirm(
                            context,
                            'Alle Alarme quittieren?',
                            'Diese Aktion beendet alle offenen Alarme auf diesem Gerät. Es wird keine Rückmeldung versendet.',
                            'Alle quittieren',
                          )) {
                            await controller.run('acknowledgeAll');
                          }
                        },
                  child: const Text('Alle quittieren'),
                )
              : null,
        ),
        for (final event in data.pending)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Card(
              elevation: 0,
              color: colors.errorContainer,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      event.isTest ? 'TESTALARM' : 'SMS-ALARM',
                      style: Theme.of(context).textTheme.labelLarge,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      event.title,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${eventTime(context, event.receivedAt)}${event.simSlot == null ? '' : ' · SIM ${event.simSlot! + 1}'}',
                    ),
                    const SizedBox(height: 12),
                    Semantics(
                      identifier: 'ack_${event.id}',
                      child: FilledButton.icon(
                        onPressed: controller.busy
                            ? null
                            : () => controller.run('acknowledge', {
                                'id': event.id,
                              }),
                        icon: const Icon(Icons.check),
                        label: const Text('Diesen Alarm quittieren'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        if (data.pendingCount > data.pending.length)
          Text(
            'Weitere ${data.pendingCount - data.pending.length} Alarme bleiben gespeichert und rücken nach.',
          ),
        if (!data.flag('audioStarted'))
          OutlinedButton.icon(
            onPressed: controller.busy
                ? null
                : () => controller.run('resumeAlarm'),
            icon: const Icon(Icons.volume_up_outlined),
            label: const Text('Offenen Alarm fortsetzen'),
          ),
      ],
      const SectionTitle('Lokaler Selbsttest'),
      NoteCard(
        title: 'Ton und Quittierung ausprobieren',
        text: 'Ein lokaler Test prüft den Alarm auf diesem Gerät. Er beweist nicht den SMS-Empfang oder das Verhalten im Hintergrund.',
        icon: Icons.volume_up_outlined,
        action: Semantics(
          identifier: 'start_test_alarm',
          child: OutlinedButton.icon(
            onPressed: !active || controller.busy
                ? null
                : () => controller.run('startTestAlarm'),
            icon: const Icon(Icons.play_circle_outline),
            label: const Text('Testalarm starten'),
          ),
        ),
      ),
      const SizedBox(height: 12),
      if (!active)
        const Text(
          'Zum Testen zuerst eine Regel anlegen und die App bewusst aktivieren.',
        ),
      const SizedBox(height: 20),
      Text(
        outcomeLabel(
          data.diagnostics['lastOutcome'] as String? ?? 'not_tested',
        ),
        style: Theme.of(context).textTheme.bodySmall,
      ),
      if (data.number('lastTestAckAt') > 0)
        Text(
          'Letzter quittierter Test: ${eventTime(context, data.number('lastTestAckAt'))}',
          style: Theme.of(context).textTheme.bodySmall,
        ),
      const SizedBox(height: 20),
      const Text(
        'Testversion: Echte Gerätetests stehen aus. Quittieren bestätigt nur den Alarm auf diesem Gerät und informiert keine Leitstelle.',
      ),
    ]);
  }

  Widget rules(AppSnapshot data) => page([
    SectionTitle(
      'Deine Alarmregeln',
      trailing: Semantics(
        identifier: 'add_rule',
        child: IconButton(
          tooltip: 'Regel hinzufügen',
          onPressed: controller.busy ? null : editRule,
          icon: const Icon(Icons.add_circle_outline),
        ),
      ),
    ),
    const Text(
      'Eine passende aktive Regel genügt. Die normale SMS-App empfängt Nachrichten weiterhin.',
    ),
    const SizedBox(height: 16),
    if (data.rules.isEmpty)
      NoteCard(
        title: 'Noch keine Regeln',
        text: 'Beginne mit einem Absender, einem Textbestandteil oder beidem.',
        icon: Icons.tune,
        action: FilledButton.icon(
          onPressed: editRule,
          icon: const Icon(Icons.add),
          label: const Text('Regel hinzufügen'),
        ),
      ),
    for (final rule in data.rules)
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Card(
          elevation: 0,
          color: Theme.of(context).colorScheme.surfaceContainerLow,
          child: Column(
            children: [
              SwitchListTile(
                title: Text(rule.name),
                subtitle: Text(
                  '${rule.summary}${rule.simSlot == null ? '' : '\nNur SIM ${rule.simSlot! + 1}'}',
                ),
                value: rule.enabled,
                onChanged: controller.busy
                    ? null
                    : (enabled) => controller.run(
                        'saveRule',
                        rule.withEnabled(enabled).toMap(),
                      ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton.icon(
                      onPressed: controller.busy ? null : () => editRule(rule),
                      icon: const Icon(Icons.edit_outlined),
                      label: const Text('Bearbeiten'),
                    ),
                    IconButton(
                      tooltip: 'Regel löschen',
                      onPressed: controller.busy
                          ? null
                          : () async {
                              if (await confirm(
                                context,
                                'Regel löschen?',
                                '„${rule.name}“ wird entfernt. Bereits offene Alarme bleiben bis zur Quittierung erhalten.',
                                'Löschen',
                              )) {
                                await controller.run('deleteRule', {
                                  'id': rule.id,
                                });
                              }
                            },
                      icon: const Icon(Icons.delete_outline),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    const SizedBox(height: 12),
    const Text(
      'Absenderkennungen können gefälscht werden. Regeln sind kein kryptografischer Echtheitsnachweis. SMS-Inhalte führen keine Befehle oder Links aus.',
    ),
  ]);
  Widget history(AppSnapshot data) => page([
    SectionTitle(
      'Lokaler Verlauf',
      trailing: IconButton(
        tooltip: 'Verlauf leeren',
        onPressed: data.history.isEmpty || controller.busy
            ? null
            : () async {
                if (await confirm(
                  context,
                  'Abgeschlossenen Verlauf löschen?',
                  'Offene Alarme bleiben erhalten. Nachrichtentexte sind hier nicht gespeichert.',
                  'Verlauf leeren',
                )) {
                  await controller.run('clearHistory');
                }
              },
        icon: const Icon(Icons.delete_sweep_outlined),
      ),
    ),
    Text(
      'Nur Metadaten · ${data.settings.retentionDays} Tage · höchstens ${data.settings.historyLimit} abgeschlossene Ereignisse',
    ),
    const SizedBox(height: 16),
    if (data.history.isEmpty)
      const NoteCard(
        title: 'Noch kein abgeschlossener Alarm',
        text: 'Quittierte und durch App-Pause beendete Ereignisse erscheinen hier. Vollständige SMS-Texte werden nicht gespeichert.',
        icon: Icons.history,
      ),
    for (final event in data.history)
      Card(
        elevation: 0,
        margin: const EdgeInsets.only(bottom: 8),
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        child: ListTile(
          leading: Icon(
            event.isTest ? Icons.science_outlined : Icons.sms_outlined,
          ),
          title: Text(event.title),
          subtitle: Text(
            '${event.statusLabel}\n${eventTime(context, event.receivedAt)} · ${event.senderLabel}'
            '${event.startedAt == null ? '' : '\nWiedergabestart: ${eventTime(context, event.startedAt!)}'}'
            '${event.latencyMs == null ? '' : '\n${event.latencyMs} ms ab SMS-Übergabe / Teststart'}'
            '${event.resolvedAt == null ? '' : '\nBeendet: ${eventTime(context, event.resolvedAt!)}'}'
            '${event.lastOutcome == null ? '' : '\n${outcomeLabel(event.lastOutcome!)}'}',
          ),
          isThreeLine: true,
        ),
      ),
  ]);
  Widget setup(AppSnapshot data) => page([
    const SectionTitle('Auf deinem Gerät einrichten'),
    const Text(
      'Aktivierung und Systemeinstellungen sind getrennt. Die App schaltet Nicht stören nicht global aus.',
    ),
    const SizedBox(height: 16),
    Semantics(
      identifier: 'request_permissions',
      child: FilledButton.icon(
        onPressed: controller.busy
            ? null
            : () => controller.run('requestCorePermissions'),
        icon: const Icon(Icons.key_outlined),
        label: const Text('SMS- und Benachrichtigungsrechte freigeben'),
      ),
    ),
    const SizedBox(height: 16),
    for (final check in data.checks)
      Card(
        elevation: 0,
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        margin: const EdgeInsets.only(bottom: 10),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    check.ok
                        ? Icons.check_circle_outline
                        : check.required
                        ? Icons.error_outline
                        : Icons.info_outline,
                    color: check.ok
                        ? Theme.of(context).colorScheme.primary
                        : null,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      '${check.title}${check.required ? '' : ' · optional'}',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(check.detail),
              const SizedBox(height: 8),
              TextButton(
                onPressed: controller.busy
                    ? null
                    : () => controller.run(
                        check.action == 'permissions'
                            ? 'requestCorePermissions'
                            : 'openSettings',
                        check.action == 'permissions'
                            ? null
                            : {'kind': check.action},
                      ),
                child: Text(check.ok ? 'Einstellung prüfen' : 'Einrichten'),
              ),
            ],
          ),
        ),
      ),
    if (data.settings.vibration && !data.flag('vibratorAvailable'))
      const NoteCard(
        title: 'Kein Vibrator erkannt',
        text: 'Auf diesem Gerät kann die App keine physische Vibration zusichern.',
        icon: Icons.vibration,
      ),
    const SectionTitle('Betriebsgrenzen'),
    const NoteCard(
      title: 'Nach Neustart und manuellem Stopp',
      text: 'Nach Geräte-Neustart zuerst entsperren. Nach „Stopp erzwingen“ die App einmal öffnen. Währenddessen nicht zugestellte SMS können nicht aus dem Postfach nachgeholt werden.',
    ),
    const SizedBox(height: 12),
    const NoteCard(
      title: 'DND, Anrufe und Audioausgabe testen',
      text: 'Telefonate, Kopfhörer, Bluetooth, Energieverwaltung und Firmenrichtlinien können Signale beeinflussen. Teste die beabsichtigten Kombinationen auf deinem Gerät.',
    ),
    const SizedBox(height: 16),
    ExpansionTile(
      title: const Text('Gerätediagnose'),
      children: [
        ListTile(
          title: Text(data.diagnostics['device'] as String? ?? 'Android-Gerät'),
          subtitle: Text(
            'Android ${data.diagnostics['androidVersion']} · API ${data.number('api')}',
          ),
        ),
        ListTile(
          title: const Text('Letzter Alarmstatus'),
          subtitle: Text(
            outcomeLabel(
              data.diagnostics['lastOutcome'] as String? ?? 'not_tested',
            ),
          ),
        ),
        if (data.diagnostics['audioWarning'] case final String warning
            when warning.isNotEmpty)
          ListTile(
            title: const Text('Audiohinweis'),
            subtitle: Text(switch (warning) {
              'volume_changed_by_user' => 'Zwischenzeitlich geänderte Systemlautstärke wurde respektiert.',
              'fixed_volume' => 'Das Gerät verwendet eine feste Lautstärke.',
              'vibration_failed' => 'Vibration konnte nicht gestartet werden.',
              _ => 'Systemlautstärke konnte nicht wie gewünscht geändert oder wiederhergestellt werden.',
            }),
          ),
        const ListTile(
          title: Text('Datenschutz'),
          subtitle: Text(
            'Keine eigenen Netzwerkverbindungen, keine Konten, keine vollständigen SMS-Texte im Verlauf.',
          ),
        ),
      ],
    ),
  ]);
}

class _UnsupportedScreen extends StatelessWidget {
  const _UnsupportedScreen();
  @override
  Widget build(BuildContext context) => const SingleChildScrollView(
    padding: EdgeInsets.all(24),
    child: NoteCard(
      title: 'SMS-Alarmierung hier nicht verfügbar',
      icon: Icons.phonelink_erase_outlined,
      text: 'Die automatische SMS-Alarmierung dieser Testversion unterstützt Android. Für iOS unter den ursprünglichen Vorgaben einschließlich Schweiz gibt es keine gleichwertig freigegebene Anbindung. Es wird kein SMS-Empfang simuliert und kein Push-Dienst eingesetzt.',
    ),
  );
}
