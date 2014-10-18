package ycf.web

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.routing.HttpService

import scala.concurrent.duration._
import scala.language.postfixOps

object BusAlertApp extends App {

  implicit val system = ActorSystem("on-spray-can")

  val service = system.actorOf(Props[BusAlertRoutingActor], "bus-alert-router")

  implicit val timeout = Timeout(5 seconds)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}

class BusAlertRoutingActor extends Actor with ActorLogging with HttpService {

  def actorRefFactory: ActorRefFactory = context

  import context.dispatcher

  val alertServiceActor = context.actorOf(Props[BusAlertServiceActor], "bus-time")

  implicit val timeout = Timeout(5 seconds)

  def receive = runRoute {
    path("") {
      get {
        complete {
          (alertServiceActor ? Start).mapTo[String]
        }
      }
    }
  }

}

/**
 * Object for providing config to application
 * Settings are contained in application.conf under ''bus-alerter''
 */
object BusAlertConfig {

  import com.typesafe.config.ConfigFactory

  private val config = ConfigFactory.load()
  private val root = config.getConfig("bus-alerter")

  val BUS_TIME_API_KEY = root.getString("BUS_TIME_API_KEY")
  val BUS_STOP_ID = root.getString("BUS_STOP_ID")
  val SMS_NUMBER = root.getString("SMS_NUMBER")

  // Receive notice for express buses.
  val EXPRESS_BUS_LINE =  //"M15-SBS"
   if(root.hasPath("EXPRESS_BUS_LINE"))
     Some(root.getString("EXPRESS_BUS_LINE"))
  else
     None

  // Range in meters that a bus must be within for a message to be sent
  val minBusRange = 350
  val maxBusRange = 750
}
