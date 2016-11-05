Vuelta Rider Tracking
==========================

This is a simple application to provide athlete tracking during an event.

This application is currently deployed at https://vuelta-rider-tracking.herokuapp.com

To run it for your own deployment you will need to set the following ENV variables:
* VUELTA_MYSQL_URL
* VUELTA_MYSQL_USER
* VUELTA_MYSQL_PASSWORD

To set these in Heroku, you can do the following:

```bash
$ heroku config:set VUELTA_MYSQL_URL=jdbc:mysql://mysql.*****:3306/vuelta
Setting config vars and restarting vuelta-rider-tracking... done
VUELTA_MYSQL_URL: jdbc:mysql://mysql.*****:3306/vuelta
$ heroku config:set VUELTA_MYSQL_USER=vueltauser
Setting config vars and restarting vuelta-rider-tracking... done
VUELTA_MYSQL_USER: vueltauser
$ heroku config:set VUELTA_MYSQL_PASSWORD=*****
Setting config vars and restarting vuelta-rider-tracking... done
VUELTA_MYSQL_PASSWORD: *****
```
