###  Ismart56 is a temperature and humidity sensor for wifi data transmission. Its json data interface can facilitate secondary development. This demo code uses SIBO Q896S tablet PC (android os 6.0) to get sensor data, compare data value control tablet GPIO LEVEL and automatically send mail.
#### Code workflow:
1. Broadcast monitors the WIFI status and gets the IP address of the tablet itself.
2. Scan all active device IP address of the same LAN.
3. Use http get to access the active IP address and get the ismart56's data.
4. Set GPIO level or send mail after comparing data
