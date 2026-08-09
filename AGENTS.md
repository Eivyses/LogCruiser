# Repository guidelines

## Project

This is a Kotlin Multiplatform project. Specifically for desktop only. Code logic is placed in shared submodule while
the desktop UI is placed in desktopApp module. The project does not and does not plan to have android and ios apps. This
is a Gradle project, so if you need to figure out dependencies they are in `build.gradle.kts` files while
`libs.versions.tml` contains all the versions of dependencies.

## Tool usage

Always prioritise MCP plugin over basic commands. If MCP for some reason is unavailable inform the user about it and
wait for it to be re-enabled. Don't run the project or tests automatically, this will be done by the user in the end.
Never ever push code yourself, nor commit. User will do it, not you.

## Coding guidelines

Project is using ktfmt plugin to format code, always format the code after you're done coding. Always try to use the
best practices possible. Avoid writing any sort of comments, only write them when they are actually very important and
explain business logic rather than what the code does.