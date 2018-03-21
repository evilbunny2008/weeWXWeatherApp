## WeeWxWeatherApp -- A weather app for WeeWx

This is community maintained project I do in my free time. Don't expect everything to be perfect and working. Rather be prepared that there are problems, as always try to fix them and contribute. If you can't fix the issue, please file a bug under issues so we can track the progress and resolution of the problem.

## Contents

 - [Screen Shots](#screen-shots)
 - [Installation](#installation)
 - [License](#license)
  - [Preparing WeeWx](#preparing-weewx)
  - [Home Screen Widget](#home-screen-widget)
 - [Credits](#credits)

## Screen Shots

<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104333.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-105034.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104351.jpg"><br>
<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104407.jpg"><br>

## Installation

### Preparing WeeWx

Unlike general weather apps, which get data from third party websites, this app gets data from personal weather stations running WeeWx. Before you can use this app, you need to prepare WeeWx with a custom template. To find out more about running WeeWx you can find details on the [WeeWx website](http://weewx.com/).

You need to download the [data.txt template](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/data.txt.tmpl) and save it into your skin directory. On a debian install this will be /etc/weewx/skin/Standard/data.txt.tmpl, this file isn't really user configurable, so just copy it in place.

From there you then need to add the template into the /etc/weewx/skin/Standard/skin.conf file, for example:
```
 [CheetahGenerator]
    [[ToDate]]
        [[[data]]]
            template = data.txt.tmpl
```
You shouldn't need to reboot or even restart WeeWx, as the skin.conf file is re-read before new reports are generated. 

If you would also like alltime statistics you also need to install the xstat.py, on a debian install you can do this by doing the following:
```
cp /usr/share/doc/weewx/examples/xstats/bin/user/xstats.py /usr/share/weewx/user/xstats.py
```
Then you need to edit /etc/weewx/skin/Standard/skin.conf and add the following line:
```
[CheetahGenerator]
    search_list_extensions = user.xstats.ExtendedStatistics
```

Next you need to download the [settings.txt](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/settings.txt) file and save it to your website in the weewx website directory, this can be either /var/www/weewx/ or /var/www/html/weewx/. You then need to customise the settings.txt file, this includes giving the URL to the data.txt file, you can also add the URL to a nearby radar image, such as from WeatherUnderground. The forecast line will use Yahoo! forecasts and you just need to enter the town/city, state/province and country you want the forecast for.

For more details on setting up and running WeeWx, check out the documentation on [WeeWX's website](http://www.weewx.com/docs/usersguide.htm)

### Setting up the app

Once you install and run the app from [Google Play](https://play.google.com/store/apps/details?id=com.odiousapps.weewxweather), the settings box will appear asking for the URL to the settings.txt file. Simply enter in the URL and click 'Save Settings', the app will then check to make sure the details in settings.txt is valid and will trigger a download of your weather information, once downloaded the app will then display the weather information.

### Home Screen Widget

If you wish to have weather on your home screen you can do this by pressing and holding on a blank area of your home screen, then there should be an option for widgets, scroll down to WeeWx Weather App, and it's a 2x1 widget. If you want up to date weather information you just need to have background downloading enabled in the app, otherwise the widget will only update when the app is running.

## License

Source code is made available under the GPLv3 license, in the hope they might be useful to others. See [LICENSE](LICENSE) for details.

## Credits

Big thanks to the <a href='http://weewx.com'>WeeWx project</a>, as this app wouldn't be possible otherwise.<br><br>
Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and <a href='https://www.yahoo.com/?ilc=401'>Yahoo! Weather Forecasts</a><br><br>This app is by <a href='https://odiousapps.com'>OdiousApps</a>.
