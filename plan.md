# LogCruiser — Refined Plan

Cross-platform desktop log viewer in Kotlin + Compose Multiplatform (JVM desktop).
Primary feature: multiple editable / deletable / togglable filters (include & exclude)
applied to lines and columns, with fast lazy rendering for large files.

---

## Verified decisions (locked in)

| Concern | Decision |
|---|---|
| Live tailing | `tail -F` follow mode; index append-only updatable + stop/pause control |
| Columns | JSON lines expose detected keys as columns; non-JSON = single `message` column (column extraction for non-JSON deferred) |
| Filter predicates | Substring + Regex, each as include or exclude; per-column or whole-line target |
| Filter composition | Includes = OR (any match passes; if no include active, all pass). Excludes = OR (any match drops) |
| Filter case sensitivity | Per-filter toggle (TBD default — open item) |
| Search | Indexed across all lines; background scan with progress + cancel; cached per-query match list |
| Indexing | `IntArray` byte offsets (≤2 GB, ≤~50 M lines → ~200 MB worst case). Single background scan on open |
| Persistence | Per-file `*.lc.json` sidecar (filters + last search) **and** global `presets.json` for reusable filter sets |
| Filter re-eval | Re-stream the file through the byte-offset index on every edit/toggle; show progress + cancel. Tail only re-evaluates appended lines for active filters |
| Encoding | UTF-8 only; offset↔decoder handled by `CharsetDecoder` per visible chunk |
| Layout | One vertical `LazyColumn`; each item is `Row { LineNumberColumn(fixed) | Body with shared HorizontalScrollState }`. Single `LazyListState` → vertical sync is free, line numbers stay pinned horizontally |
| Module layout | Keep existing `shared` (KMP) + `desktopApp` split, JVM desktop only |

---

## Memory-model correction (vs original draft)

The draft's `LogEntry` schema cannot live in memory for large files. It is redefined as a
**transient view**, constructed only for rows in the current viewport (~40–60) and discarded
on scroll:

```kotlin
data class LogEntryView(
    val rawLineIndex: Int,                       // original line index (slot in offsets array)
    val byteOffset: Int,                         // start offset in file (for re-read/seek)
    val byteLength: Int,                         // byte length of the line
    val parsedJson: String?,                     // non-null only if line validated as JSON
    val detectedColumns: Map<String, String>?,   // keys→values for JSON rows (lazy/sampled)
    val messageText: String,                     // trimmed text, ANSI preserved
)
```

Persistent state is **only**:

- the `IntArray` of line byte-start offsets
- the active filter projection (`IntArray` of visible line indexes)
- (optional, later) per-filter `BitSet` of matched line indexes for instant toggle

Raw line text is never held in memory outside the visible viewport.

---

## Phasing

### Phase 0 — MVP (lands first; no tailing, no filter persistence)

Goal: open a UTF-8 file up to ~2 GB, scroll smoothly, show correct line numbers,
single-line monospace rendering with ANSI colors, basic substring filter + plain search.

1. **`LogFileIndex` (shared jvmMain)**
   - Coroutine that scans the file and builds `IntArray` of line start byte offsets via
     chunked `InputStream` reads (no `readLine`; byte-accurate `\n` detection).
   - Reports progress; cancellable.
   - Append path for tailing stubbed (empty no-op).

2. **`OffsetLineReader`**
   - Given a line index, seek to the stored offset, decode that one UTF-8 line
     (handle `\r\n` / `\n`). No full-file buffering.

3. **Viewport state holder**
   - Holds index + `LazyListState` + horizontal `ScrollState`.
   - Builds `LogEntryView` only for visible rows.

4. **UI shell (desktopApp)**
   - Window → file picker → `LazyColumn` with pinned line-number column + monospace body.
   - `maxLines = 1`, `softWrap = false`, `FontFamily.Monospace`.

5. **ANSI parser → `AnnotatedString`**
   - Tokenise `\u001B[...m`, map basic SGR codes to `SpanStyle`.

6. **MVP filtering (substring include/exclude)**
   - Backstage `IntArray` projection that re-streams the file (with progress + cancel).
   - Excludes = OR; Includes = OR (empty include set = all-pass).

7. **MVP search (single query)**
   - Background scan → `SortedSet<Int>` match indexes.
   - Next/Prev via `lazyListState.scrollToItem(targetIndex)`.
   - Yellow highlight all matches; orange highlight current.

8. **Columns auto-detected (display only, not bound to filters yet)**
   - For JSON lines, show detected keys as a header chip strip above the viewport.

**MVP exit criteria:**

- Opens a 500 MB+ file without OOM.
- Scroll at 60 fps.
- Edit a substring include/exclude filter; list updates with progress + cancel.
- Search + jump Next/Prev works.

### Phase 1 — Filtering maturity

- **Filter model:**
  ```kotlin
  data class FilterDef(
      val id: String,
      val kind: FilterKind,            // Include | Exclude
      val type: FilterType,            // Substring | Regex
      val value: String,
      val caseSensitive: Boolean,
      val targetColumn: String? = null // null => whole-line text
  )
  ```
- **Filter list panel (Compose):** list of chips with toggle, inline edit dialog, delete;
  "Save as preset" → global `presets.json`; per-file `*.lc.json` autosave on change.
- **FilterSet** is the persistence unit; "active filters" are the enabled subset.
- **Regex** via `Regex` (compiled lazily, cached by pattern string).
- Unify Substring / Regex / include / exclude / target-column behind a `LineMatcher`
  interface:
  ```kotlin
  fun interface LineMatcher {
      fun matches(text: String, columns: Map<String, String>?): Boolean
  }
  ```

### Phase 2 — Live tail

- **`FileWatcher`** per OS via `java.nio.file.WatchService` on the parent dir
  (handles file rotation best-effort).
- **Append-only updates:** detect new bytes past last index; extend `IntArray`;
  re-evaluate active filters against appended lines only.
- **Auto-scroll behavior:** when "auto-follow" is on, jump to bottom on new lines.
  When the user scrolls up manually, **pause** auto-follow and show a
  "Jump to latest" button until clicked (open item — confirm default).
- `tail -F` semantic: re-open by path when file is truncated/recreated.

### Phase 3 — Search robustness

- Optional persisted search index (per-file sidecar), keyed by content length/hash for
  invalidation.
- Multi-match navigation (Next/Prev), wrap, case toggle, regex toggle, current-counter
  `x / N`.
- Highlight merges with ANSI `AnnotatedString`: search overlays take precedence as
  background spans while preserving SGR foreground colors.

### Phase 4 — Columns / structured view

- JSON-only path: per visible row, `Json.decodeToJsonElement` → extract columns.
- Cached column key set sampled up to first N JSON rows (with "rescan" action).
- Filter-by-column UI: pick column → bind chosen active filter to it.
- Future (deferred): user-defined extraction rules for non-JSON
  (regex named groups, delimiters).

### Phase 5 — Performance hardening

- Switch `IntArray` offsets to a memory-mapped / file-backed structure if >2 GB ever needed.
- Filtered projection cache: per-`FilterDef` `BitSet` of matched line indexes for ~free
  toggle edits at the cost of memory (enabled via setting later).

---

## Agile delivery plan (iterations)

Each iteration is a **thin vertical slice**: independently runnable, testable, and
shippable. No iteration is allowed to invalidate the work or contracts of a previous one
(extending is fine; rewriting is not). If a later iteration forces a redesign, it must
include a migration step instead of breaking earlier pieces.

Iteration `I{n}` → maps to a phase; each ends with a demonstrated, verifiable output.
"Stop point" = a natural place to pause, demo, and reassess before committing to the next
slice. Beneficial committable endpoints, not forced gates.

### Iteration I1 — Index + minimal viewport  *(→ Phase 0)*
- `LogFileIndex` builds `IntArray` of byte offsets (chunked `InputStream`, progress, cancel).
- `OffsetLineReader` decodes one UTF-8 line by offset.
- UI: window → file picker → `LazyColumn` with pinned line-number column + monospace
  single-line body (`maxLines = 1`, `softWrap = false`), shared horizontal scroll.
- **Exit criteria:** open a 500MB+ file without OOM, smooth scroll, correct line numbers.
- **Stop point:** app is already a usable read-only viewer.
**✅ DONE (2026-07-31)**
- Implemented: `LogFileIndex` (shared/jvmMain/index), `OffsetLineReader` (shared/jvmMain/index),
  `LogViewport` (desktopApp/ui), `MainScreen` with file picker + indexing progress (desktopApp/ui),
  `main.kt` wired with 1200×800 window. Unit tests in jvmTest.
- Decisions: dark theme via `MaterialTheme(colorScheme = darkColorScheme())` + `Surface` wrapper;
  line-number column 100dp `requiredWidth` with Box alignment; custom `ScrollbarStyle`
  (white-on-dark, Compose Desktop default was black-on-black); `@Preview` for all screen states.
- Shared module cleaned of KMP wizard boilerplate (App, Greeting, Platform, template resources).

### Iteration I2 — ANSI rendering  *(→ Phase 0)*
- `AnsiParser` → `AnnotatedString`; basic SGR codes mapped to `SpanStyle`.
- Wire into the viewport body; raw text fallback when no ANSI.
- **Exit criteria:** colored log lines render; no regression on scroll perf.
- **Contract preserved:** viewport reads line text via `OffsetLineReader` unchanged.

### Iteration I3 — Single substring include/exclude filter  *(→ Phase 0)*
- `FilterEngine` streams the file through the byte-offset index, evaluates one
  substring predicate (include or exclude), builds a projection `IntArray`.
- Background coroutine with **progress + cancel** (new edit cancels in-flight scan).
- Minimal filter input (one text field + include/exclude toggle); projection applied to
  the viewport.
- **Exit criteria:** typing an exclude substring drops matching lines; include works;
  progress visible; editing mid-scan cancels cleanly.
- **Contract preserved:** no UI structure changes; filter engine is additive to viewport.

### Iteration I4 — Multiple filters, toggle/edit/delete  *(→ Phase 0/1)*
- Filter list state holder (`List<FilterDef>` with stable ids).
- Filter panel: multiple chips; each togglable, editable (inline dialog), deletable.
- Composition rule implemented: Includes = OR (empty → all pass), Excludes = OR.
- Re-eval still re-streams full file on any change (progress + cancel).
- **Exit criteria:** add 5 filters, toggle/edit/delete freely; projection recomputes; no
  leaks/crashes on rapid edits.
- **Contract preserved:** `FilterDef` shape fixed here (id/kind/type/value/caseSensitive/
  targetColumn) so later iterations only extend, not break.

### Iteration I5 — Search (single query, Next/Prev)  *(→ Phase 0)*
- `SearchEngine` background scan → `SortedSet<Int>` match indexes (progress + cancel).
- Search bar; highlight all matches (yellow), current (orange); Next/Prev +
  `scrollToItem`.
- **Exit criteria:** search a large file, jump Next/Prev, count works.
- **Contract preserved:** search overlays layer **on top of** ANSI spans without altering
  the ANSI `AnnotatedString` builder.

### → MVP milestone (Phase 0 complete). Pause and reassess before tail/persistence.

### Iteration I6 — Regex matcher type  *(→ Phase 1)*
- Add `FilterType.Regex` to `FilterDef`; `RegexMatcher` implements `LineMatcher`.
- Compiles lazily, cached by pattern string.
- **Exit criteria:** regex include/exclude filters work alongside substring ones.
- **Contract preserved:** `LineMatcher` interface (introduced here) becomes the single
  predicate contract for all future matchers.

### Iteration I7 — Persistence: per-file + global  *(→ Phase 1)*
- Add `kotlinx-serialization`; `PerFileSidecarStore` (`*.lc.json`) autosaves filters + last
  search for a given file; `GlobalPresetStore` (`presets.json`) for reusable `FilterSet`s.
- "Save as preset" action in filter panel; open re-applies a file's sidecar.
- **Exit criteria:** close + reopen a file → filters last search restored; save a preset
  → apply to a different file.
- **Contract preserved:** sidecar is read on load only; missing/invalid sidecar falls back
  to empty state, never blocks opening a file.

### Iteration I8 — Live tail: append-only index + toggle  *(→ Phase 2)*
- `FileWatcher` (jvmMain) via `java.nio.file.WatchService` on the parent dir.
- On new bytes past last offset: extend `IntArray`; re-evaluate active filters against
  appended lines only (no full re-stream).
- Manual `tail -F` toggle (on/off) in UI; re-opens by path on truncation/recreate.
- **Exit criteria:** append lines to an open file from another process; they appear;
  filters apply to new lines only; toggling tail off stops updates.
- **Contract preserved:** when tail is off, behaves exactly like MVP.

### Iteration I9 — Auto-follow scroll behavior  *(→ Phase 2)*
- When auto-follow on: jump to bottom on new lines.
- When user scrolls up manually: **pause** auto-follow, show "Jump to latest" button; click
  resumes.
- **Exit criteria:** tail continues uninterrupted while paused; click jumps to latest and
  resumes following.
- **Contract preserved:** scroll-pause is UX-only; index/filter pipeline unchanged.

### Iteration I10 — Search polish  *(→ Phase 3)*
- Match counter `x / N`, wrap-around, case toggle, regex toggle.
- Search overlays merge cleanly with ANSI (background spans override, SGR colors kept).
- **Exit criteria:** toggle case/regex live; counter accurate after filter changes.
- **Contract preserved:** search result set recalculates when the active filter projection
  changes; no stale-index leaks.

### Iteration I11 — Columns display (JSON)  *(→ Phase 4)*
- For JSON lines, decode per visible row; show detected keys as a header chip strip above
  the viewport. Cached key set sampled from first N JSON rows + manual "rescan".
- **Exit criteria:** JSON log file shows columns header; non-JSON rows still single message.
- **Contract preserved:** `LogEntryView.detectedColumns` (already in model) now populated;
  non-JSON rows remain nullable as before.

### Iteration I12 — Filter-by-column  *(→ Phase 4)*
- UI: pick a column → bind a chosen active filter's `targetColumn` to it.
- `LineMatcher.matches(text, columns)` uses the bound column value when present.
- **Exit criteria:** filter by a JSON key's value; whole-line filters still work.
- **Contract preserved:** `targetColumn` (added back in I4) is finally wired; a filter with
  `targetColumn = null` behaves exactly as before.

### Iteration I13 — Projection cache (optional perf)  *(→ Phase 5)*
- Per-`FilterDef` `BitSet` of matched line indexes, enabled via setting; toggle edits
  compose bitmasks instead of re-streaming the file.
- Invalidation rules: `BitSet` stale when filter `value`/`caseSensitive`/`targetColumn`
  changes or tail appends.
- **Exit criteria:** rapid toggling of many filters is instant without re-streaming; memory
  stays bounded; tail appends invalidate only the appended segment.
- **Contract preserved:** default off → behavior identical to MVP; opt-in only.

---

## Suggested package layout

```
shared/src/commonMain/.../logcruiser/
  model/        LogEntryView, FilterDef, FilterKind, SearchMatch, ...
  filter/       LineMatcher, SubstringMatcher, RegexMatcher (interface + impls; KMP-pure if feasible)

shared/src/jvmMain/.../logcruiser/
  index/        LogFileIndex, OffsetLineReader, FileWatcher
  filter/       FilterEngine (stream file, build projection; cancel/progress)
  search/       SearchEngine (background scan, match list)
  ansi/         AnsiParser, AnsiStyleTable
  storage/      PerFileSidecarStore, GlobalPresetStore (kotlinx-serialization JSON)

desktopApp/.../logcruiser/
  ui/           MainWindow, LogViewport, LineNumberColumn, FilterPanel, FilterChip,
                SearchBar, AnsiAnnotatedString rendering
  viewmodel/    LogSessionViewModel
                (owns index + listState + horizontal scrollState + filter set + search)
```

---

## Open items to confirm before Phase 0 implementation

1. Add `kotlinx-serialization` for filters/presets sidecar? (small dependency, recommended)
2. `java.nio.file.WatchService` is JVM-only (fine — JVM desktop only target). Confirm tail
   handling being `jvmMain`-specific is acceptable.
3. Default filter case-sensitivity: **case-insensitive** by default for substring/regex
   matches, or case-sensitive? (Recommended default: insensitive for log reading.)
4. On tail auto-scroll: when user scrolls up manually, **pause** auto-follow and show a
   "Jump to latest" button until clicked? (Standard log-viewer pattern.)

---

## Implementation log

See iteration markers above for status. Next up: **Iteration I2 — ANSI rendering**.