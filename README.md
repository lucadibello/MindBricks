## Project notes for MindBricks

### Refactor (from 16.12.25 to 19.12.25)

As cited during the last lesson, it's possible to make some changes in the code before the submission of the project (scheduled for the 17th of December, 2025).
The codebase of the app is quite big, and some features were poorly implemented (not optimized, not flexible, etc). The goal of this refactor is to improve the code quality and the user experience, while also solving some bugs along the way.

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
- [x] Remove unused layout resources.
- [x] Refactored `HomeViewModel` to remove duplicated code and remove passing the same parameters multiple times to different methods and updated `HomeFragment` accordingly.
- [x] Refactored `UserPreferences` into `UserPreferencesLoader`: now we return JSON objects instead of using multiple inner classes to represent the preferences (not flexible, if the JSON changes we need to change the entire class).
- [x] Refactored `RecommendationEngine`: now using new `UserPreferencesLoader` to load user preferences, removed duplicated code, improved readability.
- [x] Refactored `CalendarIntegrationEngine`: we removed this class, and integrated its functionality directly into the `RecommendationEngine` (it was only used there, no need for an extra layer of abstraction).
- [x] Fixed ring chart in `AnalyticsFragment`: now highlighting today's sessions rather than yesterday's.
- [x] `TagManager` class had two methods doing the same thing in different ways. I merged them into a single method.
- [x] Solved bug in tags management: when adding a tag, we were not setting the correct ID to the POJO object, causing foreign key constraint exceptions when trying to save sessions with newly created tags.
- [x] Adding `@author` tags to all classes to make it clear who did what
- [x] Solved bug where we the app was still referencing the previous tag ID after the user has selected a different tag for a session.
- [x] Before we locked only the navbar and the settings menu when studying. Now we lock also the tag spinner.
- [x] Sensor sampling broke after some ~30s when phone is in standby. Using WakeLock as suggested by the Android SDK doc
- [x] More granular settings for timer (short/long/focus) as suggested during the presentation

> During this time span, I did not add any new feature to the app, just refactored existing code to improve its quality and maintainability, as well as removing some bugs/inconsistencies/messy code.

> I used Copilot to help me write javadoc comments (I always review them to ensure they are correct and make sense).
