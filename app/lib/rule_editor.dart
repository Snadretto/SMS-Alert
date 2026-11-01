// Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'models.dart';
import 'platform_gateway.dart';

class RuleEditor extends StatefulWidget {
  const RuleEditor({super.key, required this.controller, this.rule});
  final AppController controller;
  final AlarmRule? rule;
  @override
  State<RuleEditor> createState() => _RuleEditorState();
}

class _RuleEditorState extends State<RuleEditor> {
  final form = GlobalKey<FormState>();
  late final TextEditingController name, sender, text;
  final previewSender = TextEditingController(),
      previewBody = TextEditingController();
  late String logic, sim;
  late bool enabled, caseSensitive;
  bool saving = false, previewBusy = false;
  String? previewResult;
  @override
  void initState() {
    super.initState();
    final rule = widget.rule;
    name = TextEditingController(text: rule?.name ?? '');
    sender = TextEditingController(text: rule?.sender ?? '');
    text = TextEditingController(text: rule?.text ?? '');
    logic = rule?.logic ?? 'AND';
    sim = rule?.simSlot?.toString() ?? 'all';
    enabled = rule?.enabled ?? true;
    caseSensitive = rule?.caseSensitive ?? false;
  }

  @override
  void dispose() {
    for (final field in [name, sender, text, previewSender, previewBody]) {
      field.dispose();
    }
    super.dispose();
  }

  AlarmRule draft({bool preview = false}) => AlarmRule(
    id: widget.rule?.id ?? '',
    name: name.text.trim().isEmpty && preview ? 'Vorschau' : name.text.trim(),
    sender: sender.text.trim(),
    text: text.text.trim(),
    logic: logic,
    enabled: enabled,
    caseSensitive: caseSensitive,
    simSlot: sim == 'all' ? null : int.parse(sim),
  );
  Future<void> save() async {
    if (!form.currentState!.validate()) return;
    setState(() => saving = true);
    final ok = await widget.controller.run('saveRule', draft().toMap());
    if (!mounted) return;
    setState(() => saving = false);
    if (ok) {
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.controller.error ?? 'Bitte erneut versuchen.'),
        ),
      );
    }
  }

  Future<void> preview() async {
    if (sender.text.trim().isEmpty && text.text.trim().isEmpty) {
      setState(
        () =>
            previewResult = 'Mindestens einen Absender oder Suchtext angeben.',
      );
      return;
    }
    setState(() {
      previewBusy = true;
      previewResult = null;
    });
    try {
      final matched = await widget.controller.gateway.preview(
        draft(preview: true),
        previewSender.text,
        previewBody.text,
        sim == 'all' ? null : int.parse(sim),
      );
      if (mounted) {
        setState(
          () => previewResult = matched
              ? 'Diese Nachricht passt zur Regel.'
              : 'Diese Nachricht passt nicht zur Regel.',
        );
      }
    } on PlatformException catch (failure) {
      if (mounted) {
        setState(
          () => previewResult = failure.message ?? 'Vorschau nicht verfügbar.',
        );
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => previewResult = 'Die lokale Vorschau ist nicht verfügbar.',
        );
      }
    } finally {
      if (mounted) setState(() => previewBusy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(widget.rule == null ? 'Neue Alarmregel' : 'Regel bearbeiten'),
    ),
    body: SafeArea(
      child: Form(
        key: form,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Semantics(
              identifier: 'rule_name',
              child: TextFormField(
                controller: name,
                maxLength: 60,
                decoration: const InputDecoration(
                  labelText: 'Regelname',
                  hintText: 'z. B. Pikett Serverbetrieb',
                ),
                textInputAction: TextInputAction.next,
                validator: (value) => value == null || value.trim().isEmpty
                    ? 'Bitte einen Regelnamen eingeben.'
                    : null,
              ),
            ),
            const SizedBox(height: 16),
            Semantics(
              identifier: 'rule_sender',
              child: TextFormField(
                controller: sender,
                maxLength: 80,
                decoration: const InputDecoration(
                  labelText: 'Absender',
                  hintText: 'Nummer oder alphanumerische Kennung',
                  helperText: 'Optional, wenn ein Textbestandteil gesetzt ist.',
                ),
                textInputAction: TextInputAction.next,
                validator: (_) =>
                    sender.text.trim().isEmpty && text.text.trim().isEmpty
                    ? 'Absender oder Textbestandteil erforderlich.'
                    : null,
              ),
            ),
            const SizedBox(height: 16),
            Semantics(
              identifier: 'rule_text',
              child: TextFormField(
                controller: text,
                maxLength: 1024,
                minLines: 1,
                maxLines: 4,
                decoration: const InputDecoration(
                  labelText: 'Text enthält',
                  helperText:
                      'Wörtlicher Textbestandteil; keine regulären Ausdrücke.',
                ),
              ),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              initialValue: logic,
              decoration: const InputDecoration(
                labelText: 'Wenn beide Kriterien gesetzt sind',
              ),
              items: const [
                DropdownMenuItem(
                  value: 'AND',
                  child: Text('Absender UND Text müssen passen'),
                ),
                DropdownMenuItem(
                  value: 'OR',
                  child: Text('Absender ODER Text genügt'),
                ),
              ],
              onChanged: (value) => setState(() => logic = value!),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              initialValue: sim,
              decoration: const InputDecoration(labelText: 'SIM-Zuordnung'),
              items: const [
                DropdownMenuItem(value: 'all', child: Text('Alle SIMs')),
                DropdownMenuItem(value: '0', child: Text('Nur SIM 1')),
                DropdownMenuItem(value: '1', child: Text('Nur SIM 2')),
              ],
              onChanged: (value) => setState(() => sim = value!),
            ),
            const SizedBox(height: 8),
            const Text(
              'SIM-Filter nur verwenden, wenn Android die Zuordnung auf deinem Gerät liefert. Eine unbekannte Zuordnung passt nicht zu „nur SIM 1/2“.',
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Groß-/Kleinschreibung beachten'),
              value: caseSensitive,
              onChanged: (value) => setState(() => caseSensitive = value),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Regel aktiviert'),
              value: enabled,
              onChanged: (value) => setState(() => enabled = value),
            ),
            const SizedBox(height: 16),
            ExpansionTile(
              tilePadding: EdgeInsets.zero,
              title: const Text('Regel lokal ausprobieren'),
              subtitle: const Text(
                'Ohne Alarm und ohne Speicherung der Testnachricht',
              ),
              children: [
                TextField(
                  controller: previewSender,
                  decoration: const InputDecoration(
                    labelText: 'Absender der Testnachricht',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: previewBody,
                  minLines: 2,
                  maxLines: 4,
                  decoration: const InputDecoration(
                    labelText: 'Inhalt der Testnachricht',
                  ),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: previewBusy ? null : preview,
                  child: const Text('Vorschau prüfen'),
                ),
                if (previewResult != null)
                  Padding(
                    padding: const EdgeInsets.all(12),
                    child: Text(
                      previewResult!,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                const Text(
                  'Die Vorschau prüft Kriterien auch bei einer deaktivierten Regel. Sie sendet keine SMS.',
                ),
              ],
            ),
            const SizedBox(height: 24),
            Semantics(
              identifier: 'save_rule',
              child: FilledButton.icon(
                onPressed: saving ? null : save,
                icon: const Icon(Icons.check),
                label: Text(saving ? 'Wird gespeichert …' : 'Regel speichern'),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}
