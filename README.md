# 🧠 Social Media Distraction Reduction System

A Human-Computer Interaction (HCI)–inspired project that helps users reduce social media distractions through gentle interventions and gamification. Instead of completely blocking access, it encourages mindful use using friction-based design and IoT feedback.

---

## ⚙️ System Overview
This system includes:
- **Android App** — Manages focus sessions, overlays, and scoring.
- **ESP8266 + OLED Display** — Shows real-time feedback via Wi-Fi (HTTP).

Both components communicate wirelessly for smooth interaction.

---

## 📱 Android Application
- Built with **Kotlin + Jetpack Compose**
- **Overlay** blocks social apps for 60 seconds to create a “speed bump”
- **Score System (Room + KSP)**
  - +10 for starting a focus session  
  - −20 when overlay triggers from distraction
- **Wi-Fi Module** for ESP8266 connection and **RSSI-based proximity detection**

---

## 🔌 ESP8266 + OLED
- Uses **NodeMCU D1 Mini** and **SH1106 128×64 OLED**
- Displays:
  - “Welcome to Focus”
  - “Focus session started”
  - “Take a break”
  - “Focus session stopped — Score: [score]”

---

## 🚀 Future Work
- AI-based detection of excessive use  
- Wearable integration (vibration alerts)  
- Gamified leaderboards and badges  
- ESP32 + MQTT upgrade  

---

**Tech Stack:** Kotlin • Jetpack Compose • Room • Arduino • HTTP • SH1106 OLED • ESP8266

