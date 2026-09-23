# JobLink Repository Guidelines

JobLink is a professional Android job-search and recruitment portfolio application built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM, ViewModel, Coroutines, and Flow. Firebase may be added later.

## Development Guidelines

- Inspect the existing code and architecture before editing.
- Keep `MainActivity` small; place UI in screen-level composables.
- Keep business logic outside UI composables and use ViewModels for screen state where appropriate.
- Use repository abstractions for data access.
- Prefer official Android and Firebase libraries, and avoid unnecessary dependencies.
- Keep solutions simple, readable, and suitable for an undergraduate developer.
- Create reusable UI components only when they provide genuine reuse or clarity.
- Include loading, empty, and error states where appropriate.
- Preserve accessibility in UI and interaction design.
- Never hard-code passwords, API keys, or other secrets.
- Do not over-engineer simple features or modify unrelated functionality.
- Run the appropriate Gradle builds and tests after code changes, and fix errors introduced by those changes.
- Never commit or push automatically.
- After each task, briefly explain which files changed and why.
