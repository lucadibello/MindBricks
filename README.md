## Project notes for MindBricks

### Refactor (from 16.12.25 to 17.12.25)

As cited during the last lesson, it's possible to make some changes in the code before the submission of the project (scheduled for the 17th of December, 2025).
Since our app is really big, some features are poorly implemented (not optimized, not flexible, etc). The goal of this refactor is to improve the code quality and the user experience, while also solving some bugs along the way.

Here is the full list of the refactors I (@lucadibello) managed to do during in this span of time:

- [x] Split the `HomeFragment` class (HUGE, contains both UI-related code / data-related code and some helper methods) into multiple smaller classes, each with a single responsibility (`HomeViewModel` for data-related code and `HomeFragmentHelper` for helper methods).
- [x] The questionnaires are handled poorly (not using constants, emotion questionnaire answer passed to the perceived productivity questionnaire without any reason, constructors with too many parameters, etc). I refactored the questionnaire-related code to make it more modular and easier to maintain.
- [x] The code still used a database seeder, while we have a `debug` settings menu available on debug builds. I removed the class entirely.
- [x] Removed unused string resources.
- [x] Deleted old `ProfileFragment` (removed, wasn't needed anymore).
- [x] Moving hardcoded strings in layout files to string resources.
- [x] Renamed UI resource files to follow a consistent naming convention: `activity_*` for activities, `fragment_*` for fragments, `component_*` for custom views/components, `item_*` for RecyclerView items.
- [x] Splitting string resources into multiple files based on their usage (e.g., `strings_home.xml`, `strings_settings.xml`, etc).
- [x] Removed the substring "AI" from all resource files (misleading, we are not using any AI in the app. Just structured data and simple algorithms). Leftover from the initial idea of the app

- [] Adding `@author` tags to all classes to make it clear who did what
- [] Added Javadocs to all classes, methods, fields, etc.
- [] Remove unused layout resources.
