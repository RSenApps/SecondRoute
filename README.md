# SecondRoute
 A commuter-focused set-and-forget GPS that alerts you from the background if a faster route is available.

![](https://challengepost-s3-challengepost.netdna-ssl.com/photos/production/software_photos/000/174/665/datas/gallery.jpg)
![](https://challengepost-s3-challengepost.netdna-ssl.com/photos/production/software_photos/000/174/667/datas/gallery.jpg)

https://www.youtube.com/watch?v=_nrpheUmh6s


Problem: GPS's are super useful even when you know how to get from point A to point B because they will route you away from traffic. But it is a pain to have to start the gps every time you get in the car. It also drains your battery and forces you to listen to directions you already know.

Solution: Use gps, wifi, and accelerometer data to detect when you are driving. Then perform analysis on the possible routes (using Bing Maps API) to determine if you are attempting to drive home and if you are taking the optimal route. Once it detects you are driving it will periodically check if your preferred route is still the fastest. If there is a better route it will let you know and ask if you wish to start navigation. Using speech recognition and text to speech, we allow the user to keep their hands on the wheel and say "yes" or "no" to start navigation. Alternatively, we provide Android Wear integration to allow for a single tap to start navigation. We also made a completely independent Windows Phone App which provides the same functionality and utilizes the latest Windows 8.1 APIs integrated with Cortana Voice Command Technology.

Developed by: Ryan Senanayake and Joseph Zhong
