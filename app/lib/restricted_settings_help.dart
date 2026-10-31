// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'package:flutter/material.dart';

import 'platform_gateway.dart';

Future<void> showRestrictedSettingsHelp(
  BuildContext context,
  AppController controller,
) => showModalBottomSheet<void>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  showDragHandle: true,
  builder: (sheetContext) => SingleChildScrollView(
    padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Eingeschränkte Einstellungen freigeben',
          style: Theme.of(context).textTheme.headlineSmall,
        ),
        const SizedBox(height: 12),
        const Text(
          'Wenn Android „App wurde Zugriff verweigert“ meldet, musst du zunächst SMS-Alert in der App-Info freigeben. Du bestätigst diese Freigabe selbst in Android.',
        ),
        const SizedBox(height: 20),
        Semantics(
          identifier: 'restricted_menu_hint',
          label: 'In der App-Info oben rechts das Dreipunkt-Menü öffnen.',
          child: ExcludeSemantics(
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
                border: Border.all(
                  color: Theme.of(context).colorScheme.primary,
                  width: 2,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.app_settings_alt_outlined),
                  const SizedBox(width: 12),
                  const Expanded(child: Text('SMS-Alert\nApp-Info')),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    child: Icon(
                      Icons.more_vert,
                      color: Theme.of(context).colorScheme.onPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        _Step(
          1,
          'Oben rechts ⋮ öffnen',
          '„Eingeschränkte Einstellungen zulassen“ wählen. Das Menü gehört zur Hauptseite der App-Info, nicht zum SMS-Berechtigungsdialog.',
          highlight: true,
        ),
        const _Step(
          2,
          'Freigabe bestätigen',
          'Falls Android danach fragt, mit PIN oder Fingerabdruck bestätigen.',
        ),
        const _Step(
          3,
          'SMS erlauben',
          'Berechtigungen → SMS → Zulassen. Anschließend zu SMS-Alert zurückkehren und den Haken prüfen.',
        ),
        const SizedBox(height: 12),
        const Text(
          'Die Darstellung dient als Orientierung; Position und Bezeichnung können je nach Hersteller abweichen. Fehlt der Menüpunkt, zuerst die SMS-Anfrage versuchen und den Systemhinweis bestätigen, dann die App-Info erneut öffnen. Eine Firmenrichtlinie kann die Auswahl weiterhin sperren.',
        ),
        const SizedBox(height: 20),
        Semantics(
          identifier: 'open_app_info_from_help',
          child: FilledButton.icon(
            onPressed: () {
              Navigator.pop(sheetContext);
              controller.run('openSettings', {'kind': 'app'});
            },
            icon: const Icon(Icons.open_in_new),
            label: const Text('App-Info öffnen'),
          ),
        ),
      ],
    ),
  ),
);

class _Step extends StatelessWidget {
  const _Step(this.number, this.title, this.detail, {this.highlight = false});
  final int number;
  final String title, detail;
  final bool highlight;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: highlight
            ? Theme.of(context).colorScheme.primaryContainer
            : Theme.of(context).colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$number. $title',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 6),
          Text(detail),
        ],
      ),
    ),
  );
}
