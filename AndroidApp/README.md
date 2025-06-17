# AndroidApp Folder Guide

This folder contains the full Android Studio project for **Pizza Value Calculator**.

If you're exploring the codebase or want to contribute, this guide will help you quickly locate the essential files and understand the structure.

---

## Project Structure
```
AndroidApp/
├── build.gradle    # Top-level Gradle file
├── settings.gradle # Module settings
└── app/
  ├── build.gradle  # App-level Gradle configuration
  ├── src/main/
    ├── AndroidManifest.xml
    ├── java/com/howthe/pvc
    │ └── com.howthe.pvc/
    │   └── MainActivity.kt # Main body of code
    └── res/
      ├── drawable/
      | └── pizza_vector.xml
      ├── layout-land/
      |  └── activity_main.xml
      ├── layout/
      |  └── activity_main.xml
      └── values/
         └── themes.xml
```

## Key Files

- [`MainActivity.kt`](app/src/main/java/com/howthe/pvc/MainActivity.kt)  
  The core activity where all logic and UI interaction begins.

- [`activity_main.xml(vertical)`](app/src/main/res/layout/activity_main.xml) 
  [`activity_main.xml(horizontal)`](app/src/main/res/layout-land/activity_main.xml)  
  Describes the user interface layout of the main screen.

- [`pizza_vector.xml`](app/src/main/res/drawable/pizza_shape.xml)  
  Vector graphic of the pizza icon used in the interface and logo.

- [`build.gradle (:app)`](app/build.gradle.kts)  
  App-level build configuration including dependencies and SDK settings.

---

## How to Open in Android Studio

1. Open Android Studio.
2. Click on **File → Open** and navigate to the `AndroidApp/` folder.
3. Let Gradle sync finish.
4. You can now explore, build, and run the app.

---

Feel free to explore and modify, but refer to the [main README](README.md) for licensing, contribution rules, and how to download APKs.
