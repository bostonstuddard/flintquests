# Flint Quests v1.1.0 — Developer API Release

Flint Quests 1.1 makes custom integrations discoverable instead of forcing quest authors to memorize custom event strings.

## Registered custom events

Any mod can register custom-event metadata through the public API. Registered events can include a friendly name, description, provider/mod identity, subgroup, item icon, and search tags. The in-game `CUSTOM_EVENT` task picker searches that live registry.

Registration is intentionally optional: manually entered namespaced IDs and existing 1.0 `FlintQuestHooks.trigger(...)` integrations still work.

## Optional integration entrypoint

Third-party mods can declare a `flintquests` Fabric entrypoint implementing `FlintQuestIntegration`. Flint Quests invokes these entrypoints when present and automatically associates registrations with the providing mod's metadata.

This keeps the integration optional and prevents Flint Quests from accumulating hardcoded knowledge of Project Flint or any other mod.

## New public API

`FlintQuestAPI` now provides:

- custom event registration;
- event triggering with optional counts;
- registered-event lookup/listing;
- quest-completion queries;
- task-completion/progress queries;
- reward-claimed queries.

`FlintQuestEvents` exposes server-side callbacks for task progress changes, quest completion, and reward claiming.

## Compatibility

- Existing quest JSON remains compatible.
- Existing player progress remains compatible.
- `FlintQuestHooks` remains available as a deprecated compatibility facade.
- Unregistered custom events remain valid when entered manually.

See `docs/API.md` for integration examples and naming rules.
