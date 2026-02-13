# Reasoning Profile Verification Pack

This folder provides a reproducible manual validation pack for built-in reasoning profiles in the Data page.

## Verified mapping

The current mapping in application code is:

- `RDFS` -> `ReasoningProfile.RDFS` -> `RuleEngine.Profile.RDFS` -> `/rule/rdfs.rul`
- `OWL RL` -> `ReasoningProfile.OWL_RL` -> `RuleEngine.Profile.OWLRL` -> `/rule/owlrl.rul`
- `OWL RL Lite` -> `ReasoningProfile.OWL_RL_LITE` -> `RuleEngine.Profile.OWLRL_LITE` -> `/rule/owlrllite.rul`
- `OWL RL Ext` -> `ReasoningProfile.OWL_RL_EXT` -> `RuleEngine.Profile.OWLRL_EXT` -> `/rule/owlrlext.rul`

Built-in profile actions:

- Toggle switch: enable/disable profile inference.
- Eye button: open read-only preview of the built-in rule file.

See `profile-mapping.csv`.

## Datasets

Run each scenario with one file from `datasets/`:

- `rdfs-basic.ttl`
- `owlrl-symmetric.ttl`
- `owlrl-lite-transitive.ttl`
- `owlrl-ext-disjoint-union.ttl`

## Expected results

Reference values are in `expected-results.csv`.

- RDFS + `rdfs-basic.ttl` -> asserted `4`, inferred `2`, total `6`
- OWL RL + `owlrl-symmetric.ttl` -> asserted `2`, inferred `11`, total `13`
- OWL RL Lite + `owlrl-lite-transitive.ttl` -> asserted `3`, inferred `8`, total `11`
- OWL RL Ext + `owlrl-ext-disjoint-union.ttl` -> asserted `5`, inferred `10`, total `15`

## Manual validation steps

1. Open Data page.
2. Clear graph.
3. Ensure all built-in toggles and rule files are disabled.
4. Load one dataset file.
5. Enable only the matching built-in profile.
6. Compare status bar values with `expected-results.csv`.
7. Click eye button and verify opened source file path matches mapping.
