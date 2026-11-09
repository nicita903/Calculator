# CalcTerminal Pro

Phase 1 foundation for a fully offline Android mathematics app. Open this directory in Android Studio and sync Gradle. Run the app configuration on an API 24+ device. The existing AGP 9.5.1, Gradle 9.6 and SDK 37 configuration is retained.

## Implemented

AMOLED Compose Material 3 UI, left slide-out module drawer (84% width, capped at 360dp), editable arithmetic, selection-aware keypad insertion/deletion, copy/paste, session undo/redo, live preview, optional haptics stored in DataStore, and Room history (latest 200 entries). Expression and selected destination survive recreation. No internet permission, accounts, telemetry, or mathematical services. Backup is disabled.

The Gallery source was not supplied; the terminal header and blue palette follow the brief. Scientific, Equation Solver and Graphing show explicit planned screens. History, Settings and About are functional. The calculator uses a fixed five-row keypad with no vertical scroll; short landscape windows use a side-by-side display and keypad.

## Files and architecture

- math-engine/src/main/kotlin/com/calcterminal/math/ArithmeticEngine.kt: platform-independent recursive descent parser and BigDecimal arithmetic.
- math-engine/src/test/kotlin/com/calcterminal/math/ArithmeticEngineTest.kt: arithmetic regression tests.
- app/src/main/java/com/example/calculator/MainActivity.kt: edge-to-edge Activity entry point. UI is separated into ui/CalculatorApp.kt, ui/navigation/, ui/screens/ and ui/theme/.
- app/src/main/java/com/example/calculator/CalculatorViewModel.kt: StateFlow state, background calculation and persistence coordination.
- app/src/main/java/com/example/calculator/data/: Room entity, DAO, database. Java annotation processing avoids adding KSP solely for three Room types.
- app/build.gradle.kts: Compose, Room, lifecycle and DataStore dependencies.
- app/src/main/AndroidManifest.xml: launcher and offline/backup configuration.

## Arithmetic contract

Supported: +, -, *, /, ×, ÷, unary signs, decimal literals, parentheses, postfix % and bounded integer powers (^). Exponentiation is right associative and binds before unary minus: -2^2 = -4. Integer exponents are limited to -100 through 100 with result-size guards. Zero to non-positive powers and fractional exponents are rejected. Multiplication/division precede addition/subtraction. Implicit multiplication is rejected: enter 2*(3+4). A percent means division by 100: 200+10% = 200.1; 200*10% = 20. This intentionally avoids context-dependent commercial percentage rules.

Finite decimal operations are exact. Repeating division uses DECIMAL128 (34 significant digits, half-even rounding); the result and history carry an approximation marker. Approximation propagates conservatively through the expression. Limits: 512 characters and 64 nested unary/group expressions. No arbitrary code execution. Malformed/incomplete input never becomes a fabricated result; press equals for a specific error.

## Build and test (PowerShell)

~~~powershell
.\gradlew.bat :math-engine:test :app:assembleDebug :app:lintDebug
~~~

APK: app/build/outputs/apk/debug/app-debug.apk

Device checks: enter 0.1+0.2 (0.3), -(2+3)*4 (-20), 1/3 (approximation), 1/0 (error); edit in the middle, undo/redo, paste invalid text, rotate, recall/clear history, relaunch and check persistence, toggle haptics, use large fonts, and verify bottom navigation with gesture and three-button navigation.

## Next stages

Phase 2 adds powers, roots, constants, scientific notation and scientific functions with domain/precision tests. Later phases add verified equation rules, graphing and advanced mathematics. No symbolic engine is integrated yet. Symja should be evaluated separately for Android footprint, licensing, offline behavior, cancellation and supported exact operations; educational explanations require independently verified rules.
