# Decent source recipes

`cremina.json` and `best_practice.json` are unchanged copies of the source JSON archived with the profile-library review in milestone `19-profile-library-sync-v05/sources/`.

Upstream locations recorded by the editor:

- https://github.com/decentespresso/decaid/blob/main/assets/defaultProfiles/cremina.json
- https://github.com/decentespresso/decaid/blob/main/assets/defaultProfiles/best_practice.json

The source JSON supplies title, author, notes and stage parameters. Import preserves it under `recipe.source.original` and records the SHA-256 of `JSON.stringify(original)`. This hash identifies the imported JSON representation, not the original file bytes. Derived targets and conversion decisions are separate. Source transitions and limiter ranges do not establish firmware equivalence. No source recipe is silently substituted for an existing GeeFlow built-in profile.
