## WeeWxWeatherApp -- A weather app for WeeWx

This is community maintained project I do in my free time. Don't expect everything to be perfect and working. Rather be prepared that there are problems, as always try to fix them and contribute. If you can't fix the issue, please file a bug under issues so we can track the progress and resolution of the problem.

## Contents

 - [Screen Shots](#screen-shots)
 - [Installation](#installation)
 - [License](#license)
 - [Credits](#credits)

## Screen Shots

<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104333.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-105034.jpg"> <img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104351.jpg">
<img width="250px" src="https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/screenshots/Screenshot_20180321-104407.jpg"><br>

## Installation

To use this app you need a weather station, a computer to run WeeeWX software up and running, more details on that are available on the [WeeWx website](http://weewx.com/).

You need to download and customised the [settings.txt](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/settings.txt) and save it to your website in the weewx directory.

You also need to download the [data.txt.tmpl](https://raw.githubusercontent.com/evilbunny2008/WeeWxWeatherApp/master/data.txt.tmpl) and save it into your skin directory, on a debian based install this is /etc/weewx/skin/Standard/data.txt.tmpl

You then need to add the template into the /etc/weewx/skin/Standard/skin.conf file, for example:
```
 [CheetahGenerator]
    [[ToDate]]
        [[[data]]]
            template = data.txt.tmpl
```
You shouldn't need to reboot or even restart WeeWx, as the skin.conf file is re-read before new reports are generated. 

If you would like all time statistics you need to install the xstat.py, on a debian install you can do this by doing:

```
cp /usr/share/doc/weewx/examples/xstats/bin/user/xstats.py /usr/share/weewx/user/xstats.py
```
Then you need to edit /etc/weewx/skin/Standard/skin.conf and add the following line:
```
[CheetahGenerator]
    search_list_extensions = user.xstats.ExtendedStatistics
```

## License

Source code is made available under the GPLv3 license, in the hope they might be useful to others. See [LICENSE](LICENSE) for details.

## Credits

Big thanks to the <a href='http://weewx.com'>WeeWx project</a>, as this app wouldn't be possible otherwise.<br><br>
Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and <a href='https://www.yahoo.com/?ilc=401'>Yahoo! Weather Forecasts</a><br><br>This app is by <a href='https://odiousapps.com'>OdiousApps</a>.
