## GPS Mover


An advanced Android application that allows users to set and spoof their device location without enabling mock location. It is designed for use with Xposed/LSPosed modules and provides a modern, user-friendly interface for managing favorite locations, controlling location spoofing, and customizing app behavior.

---

## Features

- **Set Fake Location:**  
  Instantly set your device's location to any point on the map. The app uses Xposed/LSPosed to override system location without mock location permission.

- **Favorites Management:**  
  - Add, rename, reorder (drag & drop), and delete your favorite locations.
  - Tap a favorite to immediately set the device location and move the map.

- **Search:**  
  - Search for any coordinates directly from the app.

- **Custom Accuracy:**  
  - Control the accuracy of the reported location (e.g., 10m, 50m, etc.) from the settings.

- **Randomization Mode:**  
  - Send a randomized location around your set point (useful for bypassing some protections).

- **Map Interface:**  
  - Google Maps integration.
  - Tap anywhere on the map to select a new location.
  - Floating action buttons for quick actions (add favorite, move to real location, start/stop spoofing).

- **Import/Export Favorites:**  
  - Export all favorites to a JSON file.
  - Import favorites from a JSON file.
  - User feedback for success/failure of import/export.

- **Settings:**  
  - Switch between light, dark, and system themes.
  - Choose map type (Normal, Satellite, Terrain).
  - Adjust location accuracy.
  - Enable/disable advanced system location hook.
  - Enable/disable random position spoofing and set its range.

- **Notifications:**  
  - Persistent notification when location is set.
  - Custom notification channel.

- **Material 3 Design:**  
  - All UI elements use Material 3 colors and components.
  - Full support for light and dark modes.
  - Modern, clean, and accessible interface.

- **Xposed/LSPosed Integration:**  
  - Detects if the required module is enabled.
  - Shows clear error dialogs if not enabled.

---

## Supported Android Versions

- **Minimum:** Android 8.1 (API 27)
- **Target/Compile:** Android 14 (API 34) (Tested on android 16) 

---

## Dependencies

- Google Maps SDK
- Material Components (Material 3)
- AndroidX libraries (ViewModel, LiveData, Navigation, Room, etc.)
- Hilt (Dependency Injection)
- Timber (Logging)
- Retrofit (Networking)
- Xposed/LSPosed APIs (for system location hook)

---

## How to Use

### 1. **Initial Setup**
- Install the app on a rooted device with LSPosed or Xposed module support.
- Grant all required permissions (location, storage, etc.).
- Ensure the Xposed/LSPosed module is enabled for the app.

### 2. **Main Interface**
- The app opens to a Google Map.
- Use the floating action buttons:
  - **Add Favorite:** Tap to save the current map location as a favorite.
  - **My Location:** Move the map to your real GPS location.
  - **Start/Stop:** Begin or end location spoofing.

### 3. **Favorites**
- Access the favorites page from the bottom navigation bar.
- Tap a favorite to set your device location to that point.
- Drag to reorder, swipe to delete.

### 4. **Import/Export**
- Tap the menu button in the favorites page to import or export your favorites as a JSON file.

### 5. **Settings**
- Access settings from the bottom navigation bar.
- Change theme, map type, accuracy, enable advanced hooks, and configure random position spoofing.

### 6. **Notifications**
- When location spoofing is active, a notification will be shown.
- Stop spoofing to remove the notification.

---

## Design & Theming

- **Material 3 (Material You):**  
  All colors, backgrounds, and UI elements are derived from the Material 3 theme, supporting both light and dark modes.
- **Modern UI:**  
  Rounded corners, elevation, ripple effects, and adaptive layouts for a clean and accessible experience.

---

## Limitations & Requirements

- **Root & Xposed/LSPosed Required:**  
  The app requires a rooted device with LSPosed or Xposed module enabled to function.
- **No Mock Location Needed:**  
  The app does not use Android's mock location feature; it hooks system location directly.
- **Permissions:**  
  Requires location, storage, and notification permissions for full functionality.

---

## License

See [LICENSE](LICENSE) for details.



## Developer

Name: **Mohammed Hamham**  
Email: [dv.hamham@gmail.com](mailto:dv.hamham@gmail.com)

