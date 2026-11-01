// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'models.dart';
import 'platform_gateway.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key, required this.controller});
  final AppController controller;
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late AlarmSettings settings;
  bool saving = false;
  bool picking = false;
  @override
  void initState() {
    super.initState();
    settings = widget.controller.data!.settings;
  }

  Future<void> save() async {
    setState(() => saving = true);
    final success = await widget.controller.run(
      'updateSettings',
      settings.toMap(),
    );
    if (!mounted) return;
    setState(() => saving = false);
    if (success) {
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            widget.controller.error ??
                'Einstellungen konnten nicht gespeichert werden.',
          ),
        ),
      );
    }
  }

  Future<void> pickTone() async {
    setState(() => picking = true);
    try {
      final selected = await widget.controller.gateway.pickDeviceTone(
        settings.deviceTone?.uri,
      );
      if (mounted && selected != null) {
        setState(
          () => settings = settings.copyWith(
            tone: 'device',
            deviceTone: selected,
          ),
        );
      }
    } on PlatformException catch (failure) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              failure.message ?? 'Klingelton konnte nicht übernommen werden.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Klingeltonauswahl ist auf diesem Gerät nicht verfügbar.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => picking = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final countries = {
      'CH': 'Schweiz',
      'DE': 'Deutschland',
      'AT': 'Österreich',
      'FR': 'Frankreich',
      'IT': 'Italien',
      'NL': 'Niederlande',
      'BE': 'Belgien',
      'ES': 'Spanien',
      'GB': 'Vereinigtes Königreich',
      'US': 'USA',
      'CA': 'Kanada',
    };
    if (!countries.containsKey(settings.country)) {
      countries[settings.country] = settings.country;
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Einstellungen')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              'Alarm auf diesem Gerät',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              key: ValueKey('${settings.tone}:${settings.deviceTone?.id}'),
              initialValue: settings.tone,
              decoration: const InputDecoration(labelText: 'Alarmton'),
              isExpanded: true,
              items: [
                const DropdownMenuItem(value: 'beacon', child: Text('Signal')),
                const DropdownMenuItem(value: 'pulse', child: Text('Puls')),
                if (settings.deviceTone != null)
                  DropdownMenuItem(
                    value: 'device',
                    child: Text(
                      settings.deviceTone!.title,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
              ],
              onChanged: picking || saving
                  ? null
                  : (value) => setState(
                      () => settings = settings.copyWith(tone: value),
                    ),
            ),
            const SizedBox(height: 12),
            Semantics(
              identifier: 'pick_device_tone',
              child: OutlinedButton.icon(
                onPressed: picking || saving || widget.controller.busy
                    ? null
                    : pickTone,
                icon: const Icon(Icons.library_music_outlined),
                label: Text(
                  picking
                      ? 'Klingelton wird übernommen …'
                      : 'Klingelton vom Smartphone auswählen',
                ),
              ),
            ),
            if (settings.tone == 'device') ...[
              const SizedBox(height: 8),
              Text('Ausgewählt: ${settings.deviceTone!.title}'),
            ],
            const SizedBox(height: 8),
            const Text(
              'Die Auswahl wird mit „Einstellungen speichern“ übernommen. Der Ton wird lokal in der App gespeichert und bei einem Alarm wiederholt. Danach den lokalen Testalarm ausprobieren.',
            ),
            const SizedBox(height: 20),
            Text('Alarmtonlautstärke: ${settings.volume} %'),
            Semantics(
              label: 'Alarmtonlautstärke',
              child: Slider(
                value: settings.volume.toDouble(),
                min: 10,
                max: 100,
                divisions: 18,
                label: '${settings.volume} %',
                onChanged: (value) => setState(
                  () => settings = settings.copyWith(volume: value.round()),
                ),
              ),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Zusätzlich vibrieren'),
              value: settings.vibration,
              onChanged: (value) => setState(
                () => settings = settings.copyWith(vibration: value),
              ),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('System-Alarmlautstärke vorübergehend anheben'),
              subtitle: const Text(
                'Bewusste Freigabe: Während eines Alarms die System-Alarmlautstärke anheben. Wiederherstellung nur, wenn du sie zwischenzeitlich nicht geändert hast. Systemregeln können die Änderung blockieren.',
              ),
              value: settings.raiseSystemVolume,
              onChanged: (value) => setState(
                () => settings = settings.copyWith(raiseSystemVolume: value),
              ),
            ),
            const Divider(height: 40),
            Text(
              'Absender erkennen',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              initialValue: settings.country,
              decoration: const InputDecoration(
                labelText: 'Länderkontext für nationale Nummern',
              ),
              items: countries.entries
                  .map(
                    (country) => DropdownMenuItem(
                      value: country.key,
                      child: Text('${country.value} (${country.key})'),
                    ),
                  )
                  .toList(),
              onChanged: (value) =>
                  setState(() => settings = settings.copyWith(country: value)),
            ),
            const SizedBox(height: 8),
            const Text(
              'Für Nummern wie 079… wird dieser Länderkontext verwendet. Alphanumerische Kennungen und Kurzwahlen werden getrennt verglichen.',
            ),
            const Divider(height: 40),
            Text('Lokale Daten', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            DropdownButtonFormField<int>(
              initialValue: settings.retentionDays,
              decoration: const InputDecoration(
                labelText: 'Abgeschlossenen Verlauf aufbewahren',
              ),
              items: [7, 30, 90]
                  .map(
                    (days) => DropdownMenuItem(
                      value: days,
                      child: Text('$days Tage'),
                    ),
                  )
                  .toList(),
              onChanged: (value) => setState(
                () => settings = settings.copyWith(retentionDays: value),
              ),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<int>(
              initialValue: settings.historyLimit,
              decoration: const InputDecoration(
                labelText: 'Höchstens abgeschlossene Ereignisse',
              ),
              items: [50, 100, 200, 500, 1000]
                  .map(
                    (limit) => DropdownMenuItem(
                      value: limit,
                      child: Text('$limit Ereignisse'),
                    ),
                  )
                  .toList(),
              onChanged: (value) => setState(
                () => settings = settings.copyWith(historyLimit: value),
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              'Offene Alarme werden durch die Verlaufspflege nicht gelöscht. Vollständige SMS-Texte werden nicht gespeichert. Während App-Pause eintreffende Nachrichten werden nicht nachgeholt.',
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: saving || picking ? null : save,
              icon: const Icon(Icons.check),
              label: const Text('Einstellungen speichern'),
            ),
            const Divider(height: 40),
            Text(
              'Über SMS-Alert',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 12),
            const Text(
              'Version 0.2.0 · Android-Testversion\n\nIdee, Konzeption und Design: Snadretto.\nProgrammcode mit GPT-6.1 Sol generiert.\n\nKeine eigenen Netzwerkverbindungen, kein Konto, kein Backend, keine Telemetrie. Die SMS-Zustellung erfolgt durch Android und das Mobilfunknetz.\n\nEine Quittierung wird nicht an einen Arbeitgeber oder eine Leitstelle gesendet. Nach Neustart zuerst entsperren; nach manuellem App-Stopp einmal öffnen.',
            ),
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: () => showLicensePage(
                context: context,
                applicationName: 'SMS-Alert',
                applicationVersion: '0.2.0',
                applicationLegalese: 'Copyright 2026 Snadretto',
              ),
              icon: const Icon(Icons.description_outlined),
              label: const Text('Open-Source-Lizenzen'),
            ),
          ],
        ),
      ),
    );
  }
}
