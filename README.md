# 🌍 Boundless: Your Interactive Travel Companion

Welcome to **Boundless**! Say goodbye to boring lists and hello to a vibrant, engaging, and playful way to track your favorite spots around the globe. Whether you're saving a hidden cafe in your hometown or a breathtaking ski resort across the world, Boundless makes logging your journey as fun as the trip itself.

## ✨ The Vision (UI & UX)
Boundless isn't just a utility; it's an experience. We believe an app should be flawless, intuitive, and an absolute joy to use. 

Our Material Design interface is built around a bright, welcoming color palette designed to inspire adventure:
* **Primary:** Deep Teal (Grounding, trustworthy, reminiscent of deep oceans and forests)
* **Secondary:** Warm Orange (Energetic, sunset-inspired, draws the eye to primary actions)
* **Background:** Off-white (Clean, readable, lets the photography shine)
* **Accent:** Gold or Coral (Playful pops of color for notifications and special icons)

We utilize satisfying interactions, like collapsing parallax toolbars and smooth list scrolling, to make every tap feel responsive and alive. 

## 🚀 Core Features
Boundless is designed to be a top-performing, feature-rich application:
* **Location Tracking:** Tracks the user's current location using GPS.
* **Custom Spot Saving:** Allows you to seamlessly add and save custom GPS locations, complete with names, coordinates, and rich descriptions.
* **Smart Geo-Fencing:** Implements geo-fencing around your saved locations. When you enter within a defined radius (e.g., 200 meters), Boundless will automatically notify you.
* **Live Navigation:** Provides live directions from your current location to your selected saved point.
* **Visual Scrapbook:** Upload and attach photos to every location to keep your memories vivid.

## 🛠 Technical Architecture
Boundless is built natively for peak performance and deep system integration.

### The Stack
* **Platform:** Android (Java).
* **Minimum SDK:** Android 8.0 (API level 26).
* **UI Framework:** XML with Google's official Material Components (`MaterialCardView`, `CollapsingToolbarLayout`, `RecyclerView`).

### Key Implementations & APIs
* **Data Storage (Images & Text):** Utilizes local database architecture (SQLite/Room). To keep early prototypes highly portable, user-uploaded images are encoded and stored directly as **Base64** strings alongside location data.
* **Location Services:** Powered by the `FusedLocationProviderClient` for accurate, battery-optimized GPS tracking.
* **Background Monitoring:** Integrates the `GeofencingClient` to trigger location-based events even when the app isn't actively open[cite: 19].
* **User Alerts:** Employs the `NotificationManager` for timely, non-intrusive geo-fence alerts.
* **Mapping:** Integrates the Google Maps API for rendering interactive maps and calculating live directions.

## 📂 Project Structure
* `MainActivity.java` / `activity_main.xml`: The entry point, featuring a highly efficient `RecyclerView` to display all saved trips.
* `TripDetailActivity.java` / `activity_trip_detail.xml`: The immersive detail view utilizing CoordinatorLayout for a parallax image header.
* `MapActivity.java` / `activity_map.xml`: The full-screen interactive map view.
* `Trip.java`: The core data model representing a saved location.

---

### How to Run
1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. *Note:* To enable map rendering, you will need to insert your own Google Maps API Key into the `AndroidManifest.xml`.
4. Build and deploy to an emulator or physical Android device running API 26 or higher.