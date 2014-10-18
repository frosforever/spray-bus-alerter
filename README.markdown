## _spray_ Bus Time Alerter

This project polls the [BusTime api](http://bustime.mta.info/wiki/Developers/Index) for when a bus is within range
of a stop and sends an SMS text. It uses Spray and Akka for the endpoints and logic. 
SMS are sent using [TextBelt](http://textbelt.com/)

Follow these steps to get started:

1. The following configuration settings are passed to the application by environment variables. 
They can also be set explicitly in the `application.conf`
  1. BUS_TIME_API_KEY
  2. BUS_STOP_ID
  3. SMS_NUMBER
  4. EXPRESS_BUS_LINE (this is optional if an express bus also visits the stop)

2. Change directory into your clone:

        $ cd my-project

3. Launch SBT:

        $ sbt

5. Start the application:

        > re-start

6. Browse to [http://localhost:8080](http://localhost:8080/)

7. Stop the application:

        > re-stop
        

To run on a raspberry pi (requires `jvm` installed. For reasons I did not look into I had trouble running it with Java8):

1. Create assembly jar:

        $ sbt assembly

2. Copy created jar file to the raspberry pi

3. Set above environment variables if not explicitly specified in `application.conf`

3. Start the application:

        $ java -Xss1M -Xms64M -jar spray-bus-alerter-assembly-0.1.jar

This project was in part inspired by [Control Group](http://www.controlgroup.com/mta.html)'s work on the MTA kiosks.