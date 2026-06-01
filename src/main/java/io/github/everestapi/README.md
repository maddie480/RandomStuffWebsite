# Quick-And-Dirty Heavy Client Translation Editor

Want to edit Everest and Olympus translation using a janky Java Swing UI? This is the place!

This shows English and whatever language you want, and allows you to edit that language.

Clone Everest or Olympus, then run

```bash
java io.github.everestapi.EverestTranslationEditor <path_to_clone> <language>
```

`language` being the name of the language txt file, like `French`, or

```bash
java io.github.everestapi.OlympusTranslationEditor <path_to_clone> <language>
```

`language` being the name of the language in `lang.lua`, like `fr`.

This is here because it powers the translation viewer on the website... which is, surely, a more user-friendly way to check the translations.