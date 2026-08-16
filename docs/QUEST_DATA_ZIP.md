# Flint Quests Editable Quest Data ZIP

The Developer Environment exposes three collaboration controls in Flint Quests Settings:

- **Export Quest Data ZIP**
- **Import Quest Data ZIP**
- **Open Quest Data ZIP Folder**

Exports are written to:

```text
flintquests-exports/quest-data/
```

When **Import Quest Data ZIP** is pressed, Flint Quests uses a native file picker that starts in the current user's **Downloads** folder when it exists. The author can then choose whichever `.zip` they want to test/import.

## v2 contents / validation contract

Current exports use format `2` and contain:

```text
flintquests-data.json          REQUIRED
quests/                        REQUIRED container
categories/                    REQUIRED container
quests/*.json                  zero or more
categories/*.json              zero or more
quest_id_migrations.json       optional
theme/<active-theme>.json      optional
```

`flintquests-data.json` is the Flint Quests import marker. A current export contains at least:

```json
{
  "format": 2,
  "schema": "flintquests.quest-data.v2",
  "type": "flintquests-editable-data"
}
```

Flint Quests validates the marker **before replacing live quest data**. A ZIP fails import when, for example:

- `flintquests-data.json` is missing;
- the manifest is not valid JSON;
- `type` is not `flintquests-editable-data`;
- its format is unsupported;
- a v2 ZIP does not use `flintquests.quest-data.v2`;
- the required `quests/` or `categories/` data containers are missing;
- a ZIP entry attempts to escape the temporary extraction directory.

This means selecting a random mod ZIP, texture pack, source archive, or unrelated backup does not get treated as valid Flint Quests data.

Legacy Flint Quests v1 data ZIPs remain importable when they contain the valid v1 manifest and at least one recognized Flint Quests payload file.

## Import behavior

Before replacing quest/category data, Flint Quests automatically creates a `FlintQuests-PreImportBackup-*.zip` in the export folder. The backup happens **after validation passes**, so an invalid selected ZIP does not modify the active quest project.

Imported quest and category JSON replaces the current editable quest/category set. The included active theme is merged into the local themes folder and selected when available. Quest-ID migrations are replaced by the imported copy.

Player progress is intentionally not exported. The ZIP is authoring/project data, not a world save.


The importer intentionally does not use Swing `JFileChooser`; Minecraft can run in an environment where that throws `HeadlessException`. Selection is handled through the native LWJGL file-dialog path instead.
