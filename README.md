## WeeWxWeatherApp -- A weather app for weeWx

This is a community maintained project I do in my free time. While all efforts are taken to test everything works, in some cases things get overlooked, so don't expect everything to be working perfectly. If you spot a problem please try to fix it and contribute where you can. If you aren't able to fix the issue, please file a bug under issues so we can track the progress and resolution of the problem.

## Contents

 - [Screen Shots](#screen-shots)
 - [Concise Instructions](#concise-instructions)
 - [Expanded Installation Instructions](#expanded-installation-instructions)
   - [Preparing weeWx](#preparing-weewx)
   - [WeeWx alltime data](#weewx-alltime-data)
   - [Settings.txt](#settingstxt)
   - [Setting up the app](#setting-up-the-app)
   - [Home Screen Widget](#home-screen-widget)
 - [License](#license)
 - [Credits](#credits)

## Screen Shots

<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104333.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-105034.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104351.jpg"><br>
<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104407.jpg"><br>

## Concise Instructions

In case you want the short version, below is the concise steps:

### Step 1, install the data.txt.tmpl file in your current skin directory.
```
wget -O /etc/weewx/skins/Standard/data.txt.tmpl https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/data.txt.tmpl
```
### Step 2, update skin.conf
```
sudo nano /etc/weewx/skins/Standard/skin.conf
```
Press ctrl+w to search, then type "ToDate" (without the quotes) and hit the enter key and then enter the following:
```
        [[[data]]]
            template = data.txt.tmpl
```
The white space/indentation is needed, once you are done, press press ctrl+x to exit and save
### Step 3, enable extended statistics
```
cp /usr/share/doc/weewx/examples/xstats/bin/user/xstats.py /usr/share/weewx/user/xstats.py
```
### Step 4, create settings.txt
```
wget -O /var/www/weewx/settings.txt https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/settings.txt
sudo nano /var/www/weewx/settings.txt
```
You need to change the data= line to point to data.txt on your server which was completed in step 1, you also have the option of pointing radar= line to an image or animated image from the web. The third line, forecast= accepts a place name for forecasts. Press ctrl+x to exit and save.

### Step 5, installing the app
You can get the app from [Google Play](https://play.google.com/store/apps/details?id=com.odiousapps.weewxweather).

On first boot the app will prompt you for the URL to your settings.txt file, once entered click save and in a few seconds you should be up and running.

## Expanded Installation Instructions

### Preparing weeWx

Unlike general weather apps, which get data from third party websites, this app gets data from personal weather stations running weeWx. Before you can use this app, you need to prepare weeWx with a custom template. To find out more about running weeWx you can find details on the [weeWx website](http://weewx.com/downloads/).

You need to download the [data.txt template](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/data.txt.tmpl) and save it into your skin directory. On a debian install this will be /etc/weewx/skin/Standard/data.txt.tmpl, this file isn't really user configurable, so just copy it in place.

From there you then need to add the template into the /etc/weewx/skin/Standard/skin.conf file, for example:
```
 [CheetahGenerator]
    [[ToDate]]
        [[[data]]]
            template = data.txt.tmpl
```
You shouldn't need to reboot or even restart weeWx, as the skin.conf file is re-read before new reports are generated. 

For more details on setting up and running weeWx, check out the documentation on [weeWX's website](http://www.weewx.com/docs/usersguide.htm)

### WeeWx alltime data

If you would like alltime statistics to show up in the app you need to copy xstat.py in the user/ directory. On a debian system just do the following:
```
cp /usr/share/doc/weewx/examples/xstats/bin/user/xstats.py /usr/share/weewx/user/xstats.py
```
Then you need to edit /etc/weewx/skin/Standard/skin.conf and add the following line:
```
[CheetahGenerator]
    search_list_extensions = user.xstats.ExtendedStatistics
```
### Settings.txt

To let the app know where to download information from we have a meta config file with all the details. This saves a lot of typing, especially on radar URLs from WeatherUnderground. I have provided an example [settings.txt](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/settings.txt) file, you can save it to your website in the weewx website directory, this can be either /var/www/weewx/ or /var/www/html/weewx/.

You then need to customise the settings file, using the example as a guide only. Currently there is three lines in the file, the first is the URL to the data.txt file from above. Secondly there is an option for an animated gif file weather radar, for possible radar images view our [list of radar sources](RadarURLs.md). The third line is for forecasts from [Yahoo! Weather API](https://www.yahoo.com/?ilc=401) and you just need to enter the town/city, state/province and country you want the forecast for.

### Setting up the app

Once you install and run the app from [Google Play](https://play.google.com/store/apps/details?id=com.odiousapps.weewxweather) the settings box will appear asking for the URL to the settings.txt file. Simply enter in the URL and click 'Save Settings', the app will then check to make sure the details in settings.txt is valid and will trigger a download of your weather information, once downloaded the app will then display the weather information.

### Home Screen Widget

If you wish to have weather on your home screen you can do this by pressing and holding on a blank area of your home screen, then there should be an option for widgets, scroll down to weeWx Weather App, and it's a 2x1 widget. If you want up to date weather information you just need to have background downloading enabled in the app, otherwise the widget will only update when the app is running.

## License

Source code is made available under the GPLv3 license, in the hope they might be useful to others. See [LICENSE](LICENSE) for details.

## Credits

Big thanks to the <a href='http://weewx.com'>weeWx project</a>, as this app wouldn't be possible otherwise.<br><br>
Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and <a href='https://www.yahoo.com/?ilc=401'>Yahoo! Weather Forecasts</a><br><br>This app is by <a href='https://odiousapps.com'>OdiousApps</a>.
