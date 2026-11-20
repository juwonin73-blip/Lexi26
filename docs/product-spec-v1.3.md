# Lexi26 v1.3 Product & Technical Design
## Direction
Lexi26 becomes a goal-based multilingual vocabulary trainer. Learners do not manually enter every word: they choose a target language, current level, target level, and deadline; Lexi26 builds a vocabulary plan and imports suitable entries from dictionary data sources.

## Product principles
- Multilingual by design: language is data, not a hard-coded screen or database schema.
- Automatic acquisition: select candidate lemmas from language packs using level, frequency, topic, and learner progress.
- Offline-first: download a language pack once; study, review, and track progress offline afterward.
- Transparent provenance: every imported entry stores its source and license/attribution metadata.
- CEFR honesty: CEFR describes communicative proficiency; it does not prescribe a universal official word count or a single official word list. Level tags must be attributed to a named corpus/list or marked as Lexi26 estimates, never presented as official CEFR vocabulary counts.
- Preserve learner control: users can skip, suspend, or remove words; automatic collection must not silently add duplicates or overwhelm the daily workload.

## Learner flow
1. Select study language (for example French, English, Japanese, German, Spanish, Italian, Korean, Chinese, Arabic, etc.).
2. Complete a short placement check or select a self-assessed starting level.
3. Select target level (A1–C2 where a reliable level inventory exists) or a custom target vocabulary size.
4. Select deadline and available study time per day.
5. Lexi26 estimates a daily new-word quota and a separate review workload.
6. The app selects words from the downloaded language pack, prioritizing appropriate level, frequency, and useful coverage while excluding known/skipped words.
7. The learner studies with 26-second skimming, flashcards, pronunciation/TTS, and spaced repetition.
8. The plan adapts: when due reviews accumulate, reduce new words; when workload is light, keep or modestly increase the quota. Show the reason for any adjustment.

## Data source strategy
### Primary dictionary data
Evaluate Kaikki.org / Wiktextract structured Wiktionary data as the first multilingual source. It exposes machine-readable dictionary datasets for many languages and includes definitions, parts of speech, pronunciation and other fields where available. Coverage and field completeness vary by language and edition.

- Do not bundle the complete raw Wiktextract corpus in the APK: raw downloads are multi-gigabyte.
- Build compact, language-specific packs from the structured data. Each pack contains normalized entries and source metadata.
- Keep dictionary facts (definitions, pronunciation, examples, inflections) separate from learning metadata (frequency rank, level band, topic, selection weight).
- Treat level/frequency inventories as separate licensed or openly documented datasets. Where no defensible CEFR list exists, use frequency bands and label level as unavailable/estimated.
- Provide source links and attribution in an About > Data sources screen and in each pack manifest.

### Licensing and attribution
Kaikki states its extracted data follows Wiktionary's CC BY-SA and GFDL licenses. Before redistributing a pack, comply with the applicable attribution and share-alike/GFDL obligations, include source links and license notices, and review third-party media/audio rights separately. Prefer text-only initial packs; link to audio or use Android TTS unless the audio asset's license is verified.

## Architecture
- DictionaryProvider: common interface for lookup and pack import; provider-specific parsing stays behind adapters.
- LanguagePack: versioned, downloadable, compressed data bundle for one target language; includes language code, source edition, dataset date, license, attribution URL, and schema version.
- Lexeme: stable ID, language code, lemma, part of speech, IPA (optional), definitions/translations, example (optional), inflections (optional), source URL, source/license metadata.
- LearningProfile: target language, starting level, target level/custom word count, start date, deadline, daily time budget.
- WordProgress: lexeme ID, status (new/learning/known/suspended), ease, repetitions, interval, due date, last reviewed.
- LevelInventory: lexeme IDs with provenance and classification method (published list, frequency-derived estimate, or unclassified).
- PlanEngine: estimates new words/day and review load, schedules daily sessions, and adjusts quota based on backlog and learner responses.
- PackManager: downloads/verifies/imports packs, checks versions, handles storage, and supports offline use.
- SQLite remains the local source of truth for user progress. Dictionary packs can live in app-private storage and be imported/indexed locally.

## Planning logic
- Let N be the number of eligible target lexemes not already known, and D the remaining study days. The baseline new-word target is ceil(N / D).
- Apply a configurable daily time budget and estimated review cost to avoid unrealistic quotas.
- Keep new words and due reviews as separate numbers in the daily plan.
- If the due-review backlog exceeds the user's configured capacity, temporarily reduce new words rather than allowing review debt to grow unchecked.
- Never imply that memorizing a word list alone guarantees a CEFR level; display the goal as a vocabulary-learning target that supports broader proficiency.

## Language rollout
The data model and provider interfaces support multilingual packs from the beginning. Initial release should ship a small set of validated packs first (e.g. English, French, German, Spanish, Japanese), then expand based on source quality. A language may be selectable only when its pack has enough verified entries and metadata. Do not claim uniform CEFR coverage across all languages.

## v1.3 milestones
1. Add language/goal setup screens and persist LearningProfile.
2. Add provider and pack-manifest schema; add source/license attribution screen.
3. Build a small validation pipeline for two pilot languages and inspect actual field coverage.
4. Add pack download/import and offline search/indexing.
5. Add level/frequency-based candidate selection and daily plan generation.
6. Connect generated sessions to the existing 26-second flow and spaced-repetition engine.
7. Add tests for duplicate prevention, missing translations, language-specific scripts, database migration, pack updates, and no-network offline study.

## Important constraints
- The current v1.1 app has manual word entry and SQLite progress only. This specification describes the next version; it does not claim these features are already implemented.
- The current Android app has no network permission. Pack download will require a carefully scoped network permission and must remain optional; all study functionality must work offline after import.
- Validate Android SDK/Gradle build and test on a device before distributing a new APK.
