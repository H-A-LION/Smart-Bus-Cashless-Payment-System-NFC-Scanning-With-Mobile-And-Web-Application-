** Project Title: SmartBus Cashless Payment System with NFC Scanning

** Problem Statement:

Urban transportation systems, particularly public buses, often rely on outdated payment mechanisms such as cash transactions or paper tickets, which lead to inefficiencies like long wait times, cash handling errors, and difficulties in tracking payments and passenger data. These systems also limit the adoption of more advanced, convenient, and secure payment methods, hindering the modernization of public transport. Additionally, there is a growing demand from passengers for real-time information about buses, such as balance checks, payment history, and estimated arrival times.

The SmartBus Cashless Payment System aims to address these challenges by creating a seamless, secure, and user-friendly digital solution that leverages **NFC technology** for contactless fare payments and integrates **GPS tracking** for real-time bus monitoring. The system includes:

1. Bus-side Mobile App: A mobile app installed on the bus, which is equipped with an NFC reader and GPS tracking. This app facilitates the scanning of NFC-enabled cards (or smartphones) for payments and sends the NFC card ID along with GPS data to Firebase Firestore.

2. Customer Mobile App: A mobile app for passengers to log in, view their balance and transaction history, and select the bus station they are waiting for. The app will also provide real-time updates on the estimated time of arrival (ETA) of the bus.

3. Admin Web App: A web-based platform that enables administrators to manage customer accounts, register new users, view account details, and track buses in real-time. Admins can also indicate bus station locations on a map (using OpenStreetMap) and monitor the current location of buses.

** Objectives:

1. Contactless Payments: Develop a mobile app for buses that integrates NFC scanning to enable quick, secure, and contactless payment processing. The system will automatically send transaction details (including NFC card IDs) to Firebase Firestore for secure storage and processing.

2. Real-Time GPS Tracking: The mobile app installed on the buses will also gather GPS data, sending real-time location updates to Firebase to track buses on the map and provide accurate ETAs for passengers.

3. Passenger Convenience: Develop a customer mobile app that allows users to easily log in, check their balance, review their payment history with timestamps, and select their current bus station to receive real-time bus arrival updates.

4. Admin Control: Implement a web app for administrators to manage customer accounts, edit details, and track bus locations on the map. The admin app will use OpenStreetMap for station visualization and will allow the admin to interact with the system in real-time.

5. Scalability and Integration: Ensure the system is scalable and can easily be integrated with existing bus networks, supporting both small and large public transportation operations.

6. Security and Efficiency: Implement secure authentication and data storage solutions to protect user information and payment details. The system must also ensure that all NFC transactions are fast, efficient, and accurate.

** Key Features:

1. Mobile App for Buses:
   - NFC reader integration for fare collection.
   - GPS tracker to capture bus location.
   - Real-time data transmission to Firebase (payment details and GPS data).
   - Seamless payment confirmation and user transaction notifications.

2. Mobile App for Passengers:
   - User registration and login.
   - Real-time balance and transaction history with timestamps.
   - Station selection for waiting passengers.
   - ETA updates for the next bus arrival.

3. Web App for Admin:
   - Admin user registration and login.
   - Manage customer accounts, including the ability to view, edit, and delete accounts.
   - Integration with OpenStreetMap to display bus stations and bus locations in real-time.
   - Ability to register new customers and monitor transaction data.

** Benefits:

- Efficiency: Streamlining the fare payment process with NFC eliminates cash handling, reduces transaction times, and improves operational efficiency.
- Convenience: Passengers can quickly and easily pay for their fare without the need for physical tickets or cash. They also benefit from real-time updates about bus arrivals and their transaction history.
- Security: By using NFC technology and Firebase, the system ensures that transactions are secure and easily traceable.
- Real-Time Data: GPS tracking allows both passengers and administrators to monitor bus locations, making the entire experience more transparent and predictable.
- Scalability: The solution is scalable and can be easily adapted for larger cities or different bus routes by updating the web interface and mobile applications.

** Conclusion:

The SmartBus Cashless Payment System using NFC technology will improve the efficiency of fare collection, enhance the user experience with real-time bus tracking and payment history, and provide administrators with a robust tool for managing the system. By eliminating traditional payment bottlenecks and offering a modern, scalable solution, this project will pave the way for a more streamlined and user-friendly urban transportation system.

Reference: [CENG495_CCE_Final Report_Hussein_Mahmod.odt]https://github.com/H-A-LION/Smart-Bus-Cashless-Payment-System-NFC-Scanning-With-Mobile-And-Web-Application-/blob/main/CENG495_CCE_Final%20Report_Hussein_Mahmod.odt
