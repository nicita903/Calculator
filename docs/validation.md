# Drawer redesign validation

Build command: `.\gradlew.bat :math-engine:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --console=plain`

Result: BUILD SUCCESSFUL. Eight engine regression tests passed; existing app unit tests passed; lint has no errors (dependency/deprecation and other advisory warnings remain).

Device: Samsung SM-S9310, 1080 x 2340. Installed the debug APK preserving existing app data. Checked the full keypad above the gesture bar, no vertical calculator ScrollView, expression 7+8, drawer opening, outside dismissal, Back dismissal, Scientific placeholder and Back return, expression retention across module/drawer changes, and swipe opening from inside content. Very-edge swipes are reserved for Android system Back.

Screenshot: ../calculator-screen.png

Not device-tested: tablets, split screen, all font scales, landscape, three-button navigation, rotation/process recreation and animation frame rates. State restoration uses SavedStateHandle for expressions and rememberSaveable for the selected module/editor. Scientific, Equation Solver and Graphing remain explicit placeholders.
