# SmartBus-Payment-Arduino-Android-App-Web-App

## Project Proposal:

### Smart Bus Payment System: Arduino Implementation with Mobile Application Connected Via Wi-Fi

### Project Description:

This project is a cashless bus payment system that integrates GPS-tracked buses, QR-based payments, and real-time bus arrival updates. It consists of two main interfaces:

#### 1. Customer View (Android App)

- Customers log in through the Android app.
- Customers can select a station through a **spinner** to check estimated bus arrival time.
- GPS data from each bus (connected to an Arduino) provides real-time location updates.
- Customers use their phone camera to **scan a QR code** for fare payment.
- The app shows balance, transaction history, and payment confirmation.
- Customers use a smart card to pay for bus rides instead of cashes.
- The card can be recharged at specific offices and linked to mobile app.
- Receive notifications about card usage.

#### 2. Admin View (Web App)

- Admins log in through the **web application**.
- They can **manage financial transactions**, including charging customer accounts.
- Admins can **edit and manage station locations**.
- The system keeps a record of all transactions.
- Admins can monitor buses on a map based on their GPS data.

### Technical Implementation:

- **Arduino Uno R3** is used in buses, connected to **GPS (Neo-6MV2)** and **Wi-Fi (ESP-01 8266)**.
- GPS in buses sends real-time location data to the **server**, which the Android app uses to display estimated bus arrival times.
- Customers **scan a QR code** through the Android app for **cashless fare payment**.
- The **admin web app** is used to manage payments, transactions, and station locations.

### Database & Connectivity:

- The system uses **Firebase** \*\* server\*\* for data storage.
- The Android app and web app communicate with the database via a **REST API**.
- **OpenStreetMap (OSM)** provides map services for tracking buses and station locations.

### Additional Features:

- Customers can view transaction history and payment confirmations in the app.
- Logout & account switch options are available.

This project enhances public transport efficiency by integrating real-time tracking, mobile payments, and administrative management into a seamless system.

