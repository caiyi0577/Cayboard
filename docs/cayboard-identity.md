# Cayboard identity and migration boundary

Cayboard is the product name and the Android application/package namespace is `com.cayboard`. App-owned resource roots and persistent input data use Cayboard names such as `cayboard_base`, `cayboard_chinese`, `cayboard_pinyin`, and `cayboard_input`.

## Engine and upstream attribution

The current input engine is librime. Its public API, native target names, schema/package formats, and binary table formats remain engine contracts; renaming those identifiers would require replacing or forking the engine rather than a product-name cleanup. Source and license attribution for CyIME, Xime, librime, and other bundled components remains in `NOTICE.txt`, `LICENSE`, and the About screen's dependency list.

## Upgrade compatibility

The `com.cayboard` package name and Cayboard data paths are active. `LegacyNames` and `LegacyNameMigration` retain narrowly scoped readers for older CyIME/Xime installations, including the former `files/rime` data directory, `xime.yaml` configuration files, `kime_settings`, and prior resource identifiers. Those aliases are migration inputs, not active product names. When an old and new file coexist, migration preserves the old file in the conflict archive instead of overwriting either copy.

## Historical records

Version notes written when the product was called CyIME retain that historical name. They describe the package and behavior delivered at the time and are not current branding instructions. Current product and engine boundaries are documented here.
