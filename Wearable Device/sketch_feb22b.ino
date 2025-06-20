#include <Wire.h>
#include <U8g2lib.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

// Constants and configurations
const char* ssid = "Redmi Note 10S";
const char* password = "12345678";
const char* phoneIP = "10.119.53.246";
const unsigned long PING_INTERVAL = 5000;
const int RSSI_THRESHOLD = -40;
bool initialWarningShown = false;

ESP8266WebServer server(80);
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, /* reset=*/ U8X8_PIN_NONE);

// Improved message queue structure
const int MAX_MESSAGES = 5;
struct MessageQueue {
    String messages[MAX_MESSAGES];
    unsigned long displayTimes[MAX_MESSAGES];
    int count = 0;
    int currentIndex = -1;
};

MessageQueue messageQueue;

void addMessage(String message, unsigned long displayTime) {
    // If queue is full, shift all messages left
    if (messageQueue.count >= MAX_MESSAGES) {
        for (int i = 0; i < MAX_MESSAGES - 1; i++) {
            messageQueue.messages[i] = messageQueue.messages[i + 1];
            messageQueue.displayTimes[i] = messageQueue.displayTimes[i + 1];
        }
        messageQueue.count--;
    }
    
    // Add new message at the end
    int newIndex = messageQueue.count;
    messageQueue.messages[newIndex] = message;
    messageQueue.displayTimes[newIndex] = millis() + displayTime;
    messageQueue.count++;
    
    // If this is the first message, display it immediately
    if (messageQueue.currentIndex == -1) {
        messageQueue.currentIndex = 0;
        displayText(message);
    }
}

void handleMessages() {
    if (messageQueue.count > 0 && messageQueue.currentIndex >= 0) {
        // Check if current message's display time has expired
        if (millis() >= messageQueue.displayTimes[messageQueue.currentIndex]) {
            // Move to next message
            messageQueue.currentIndex++;
            
            // If we've shown all messages, reset the queue
            if (messageQueue.currentIndex >= messageQueue.count) {
                messageQueue.count = 0;
                messageQueue.currentIndex = -1;
            } else {
                // Display the next message
                displayText(messageQueue.messages[messageQueue.currentIndex]);
            }
        }
    }
}

// Rest of your existing functions remain the same
// void displayText(String message) {
//     u8g2.clearBuffer();
//     u8g2.setFont(u8g2_font_ncenB08_tr);
    
//     int yPos = 10;
//     int spaceLeft = message.length();
//     String currentLine = "";
    
//     for (int i = 0; i < message.length(); i++) {
//         if (message[i] == '\n' || i == message.length() - 1) {
//             if (i == message.length() - 1) currentLine += message[i];
//             u8g2.drawStr(0, yPos, currentLine.c_str());
//             currentLine = "";
//             yPos += 12;
//         } else {
//             currentLine += message[i];
//         }
//     }
    
//     u8g2.sendBuffer();
// }

void displayText(String message) {
    u8g2.clearBuffer();
    
    // Split message into lines
    int maxCharsPerLine = 16;  // Approximate chars that fit with u8g2_font_ncenB10_tr
    int maxLines = 5;          // Maximum lines that fit on display
    String lines[maxLines];
    int lineCount = 0;
    
    // Process message into lines
    String currentLine = "";
    String words[30];  // Array to store words
    int wordCount = 0;
    
    // Split message into words
    String currentWord = "";
    for (int i = 0; i <= message.length(); i++) {
        if (i == message.length() || message[i] == ' ' || message[i] == '\n') {
            if (currentWord.length() > 0) {
                words[wordCount++] = currentWord;
                currentWord = "";
            }
            if (i < message.length() && message[i] == '\n') {
                words[wordCount++] = "\n";
            }
        } else {
            currentWord += message[i];
        }
    }
    
    // Process words into lines
    for (int i = 0; i < wordCount && lineCount < maxLines; i++) {
        if (words[i] == "\n") {
            if (currentLine.length() > 0) {
                lines[lineCount++] = currentLine;
                currentLine = "";
            }
        } else if (currentLine.length() + words[i].length() + 1 <= maxCharsPerLine) {
            if (currentLine.length() > 0) currentLine += " ";
            currentLine += words[i];
        } else {
            lines[lineCount++] = currentLine;
            currentLine = words[i];
        }
    }
    if (currentLine.length() > 0 && lineCount < maxLines) {
        lines[lineCount++] = currentLine;
    }
    
    // Calculate vertical spacing
    int totalHeight = 64;  // Display height
    int lineHeight = 12;   // Height per line
    int startY = (totalHeight - (lineCount * lineHeight)) / 2 + lineHeight; // Center vertically
    
    // Draw each line centered
    u8g2.setFont(u8g2_font_ncenB10_tr);  // Larger, more attractive font
    
    for (int i = 0; i < lineCount; i++) {
        int strWidth = u8g2.getStrWidth(lines[i].c_str());
        int startX = (128 - strWidth) / 2;  // Center horizontally
        u8g2.drawStr(startX, startY + (i * lineHeight), lines[i].c_str());
    }
    
    u8g2.sendBuffer();
}

bool isPhoneOnline(const char* host) {
    IPAddress resolvedIP;
    return WiFi.hostByName(host, resolvedIP);
}

int getRSSI() {
    return WiFi.RSSI();
}

void setup() {
    Serial.begin(115200);
    u8g2.begin();
    u8g2.enableUTF8Print();
    
    displayText("Connecting to\nWiFi...");
    
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    
    Serial.println("\nWiFi Connected!");
    Serial.print("Connected to WiFi. IP address: ");
    Serial.println(WiFi.localIP());
    
    addMessage("WiFi Connected!\nIP: " + WiFi.localIP().toString(), 5000);
    
    server.on("/start", []() {
        int rssi = getRSSI();
        if (rssi <= RSSI_THRESHOLD) {
            addMessage("Time to focus!\nDon't use your phone!", 5000);
            addMessage("Focus session has\nstarted.Be mindful!!!", 10000);
        } else {
            addMessage("Focus session\nhas started", 5000);
        }
        server.send(200, "text/plain", "OK");
    });
    
    server.on("/overlay", []() {
        addMessage("Take a break\nDon't get distract by social media", 10000);
        addMessage("Focus session\nhas started", 20000);
        server.send(200, "text/plain", "OK");
    });
    
    server.on("/stop", []() {
        String score = server.arg("score");
        addMessage("Focus session\nhas stopped\nYour score is " + score, 5000);
        server.send(200, "text/plain", "OK");
    });
    
    server.on("/get_rssi", HTTP_GET, []() {
        server.send(200, "text/plain", String(WiFi.RSSI()));
    });
    
    server.begin();
}

void loop() {
    server.handleClient();
    handleMessages();

    static unsigned long lastPingTime = 0;
    unsigned long currentTime = millis();
    
    if (WiFi.status() != WL_CONNECTED) {
        addMessage("WiFi Lost!", 1000);
        Serial.println("WiFi connection lost! Reconnecting...");
        WiFi.reconnect();
        return;
    }
    
    if (currentTime - lastPingTime >= PING_INTERVAL) {
        lastPingTime = currentTime;
        if (isPhoneOnline(phoneIP)) {
            int rssi = getRSSI();
            Serial.printf("RSSI: %d dBm\n", rssi);
        }
    }
}