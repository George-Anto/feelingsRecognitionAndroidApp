# Feelings Recognition

**Introduction**

Welcome to Feelings Recognition, a mobile application designed to serve as an input component within a larger system focused on collecting, storing, and processing facial expressions during video consumption. This Kotlin-based Android app seamlessly integrates with Firebase, leveraging its Authentication, Firestore, and Storage functionalities. The primary goal of Feelings Recognition is to analyze user reactions to videos, either from a curated collection stored in Firebase or sourced from YouTube. Users can sign up with their email and password, edit their profile details, and upload an image. The app activates the selfie camera while users watch selected videos, capturing their facial expressions. After video completion, the facial data is stored, and for YouTube videos, relevant data is sent to an API for comprehensive analysis.

## Table of Contents

-   [Prerequisites](#prerequisites)
-   [Configuration](#configuration)
-   [Dependencies](#dependencies)
-   [Build and Run](#build-and-run)

## Prerequisites

Before setting up the app, ensure you have the following:

-   Android Studio installed on your computer.
-   A Firebase account with an existing project to link to the app.
-   A YouTube API key.

## Configuration

1. **Firebase Configuration:**

    - The project must be linked to a Firebase project, and it utilizes Firebase Authentication, Firestore, and Storage functionalities. Ensure a Firebase project of your choosing is created and correctly set up and link it to this app.

2. **YouTube API Key:**

    - Create a YouTube API key and add it to a file named `credentials.properties` in the `assets` folder. The property should be named `youtube.api.key`.


    ```properties
    youtube.api.key=YOUR_YOUTUBE_API_KEY
    ```

## Dependencies

-   Kotlin Plugin Version: 1.7.10-release-333-AS5457.46
-   Java Version: 1.8

## Build and Run:

-   Open the project in Android Studio.
-   Make sure that all the nessacary steps, as explained in **Prerequisites**, **Configuration** and **Dependencies**, are followed.
-   Press the run icon to build and run the app.
