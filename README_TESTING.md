# VendorConnect Testing Guide

This document provides instructions for testing the GPS navigation and vendor search functionality in the VendorConnect app.

## 1. Testing the Directions Button

The Directions button has been fixed to properly navigate to vendor locations using Google Maps.

### Manual Testing Steps:

1. Launch the VendorConnect app and log in as a customer
2. Grant location permissions when prompted
3. Select any vendor from the list or map
4. Tap the "Directions" button
5. Verify that Google Maps opens with directions to the vendor
6. If Google Maps is not installed, verify that a web browser opens with Google Maps directions
7. If neither option works, verify that a generic map intent opens

### Expected Results:
- The app should attempt to open Google Maps with your current location as the starting point
- If your location is not available, it should still navigate to the vendor's location
- You should not see any error messages or crashes

## 2. Testing Locality Search

### Manual Testing Steps:

1. Launch the VendorConnect app and log in as a customer
2. In the locality search bar, type a locality name (e.g., "Andheri", "Juhu", "Vashi")
3. Press the search button on the keyboard
4. Verify that only vendors in the specified locality are displayed
5. Try partial locality names to verify fuzzy matching works

### Expected Results:
- Only vendors in the matching locality should be displayed
- The map should update to show only the filtered vendors
- A toast message should show the number of vendors found

## 3. Testing Radius-Based Search

### Manual Testing Steps:

1. Launch the VendorConnect app and log in as a customer
2. Grant location permissions when prompted
3. Use the radius slider to set a search radius (e.g., 2km, 5km, 10km)
4. Verify that only vendors within the specified radius are displayed
5. Try different radius values to see how results change

### Expected Results:
- Only vendors within the specified radius from your current location should be displayed
- Vendors should be sorted by distance (nearest first)
- The map should update to show only the filtered vendors
- A toast message should show the number of vendors found

## 4. Testing Category and Open-Now Filters

### Manual Testing Steps:

1. Launch the VendorConnect app and log in as a customer
2. Tap on category filter chips (e.g., "Restaurant", "Retail", "Service")
3. Verify that only vendors in the selected categories are displayed
4. Tap the "Open Now" chip
5. Verify that only currently open vendors are displayed
6. Try combining filters (e.g., "Restaurant" + "Open Now" + 5km radius)

### Expected Results:
- Only vendors matching all selected filters should be displayed
- The map should update to show only the filtered vendors
- A toast message should show the number of vendors found

## 5. Unit Testing

Unit tests for the search functionality are available in `SearchFunctionalityTest.kt`. These tests verify:

- Filtering by vendor name
- Filtering by locality
- Filtering by category
- Filtering by open status
- Filtering by radius

To run the unit tests:

1. Open the project in Android Studio
2. Right-click on the `SearchFunctionalityTest.kt` file
3. Select "Run 'SearchFunctionalityTest'"

All tests should pass, confirming that the search filtering logic works correctly.

## Troubleshooting

If you encounter any issues during testing:

- Ensure location permissions are granted
- Check that Google Maps is installed for the best navigation experience
- Verify that you have an active internet connection
- If the app crashes, check the logcat output for error messages