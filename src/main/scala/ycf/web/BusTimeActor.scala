package ycf.web

import akka.actor._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import ycf.web.BusTimeResponseJsonProtocol._

import scala.concurrent.duration._

/**
 * Message to send [[BusAlertServiceActor]] to trigger a check for buses
 */
case object Start

/**
 * Message to trigger a check with Bustime and in turn an SMS if appropriate
 */
case object CheckForBus

case class Bus(distance: Distances, lineName: String, progressRate: String)

/**
 * On receipt of [[CheckForBus]], calls the [[http://bustime.mta.info BusTime Api]] for the stop set in the config.
 *
 * Munges response from [[BusTimeResponse]] to [[Bus]]. If none are available, schedules
 * another check in 30 seconds. If one is found, passes to [[SmsSenderActor]] for notification.
 *
 */
class BusAlertServiceActor extends Actor with ActorLogging {

  import context.dispatcher

  val busTimeUri = s"http://bustime.mta.info/api/siri/stop-monitoring.json?" +
    s"key=${BusAlertConfig.BUS_TIME_API_KEY}&OperatorRef=MTA" +
    s"&MonitoringRef=${BusAlertConfig.BUS_STOP_ID}"

  val smsSenderActor = context.actorOf(Props[SmsSenderActor], "sms-sender")

  override def receive = ready

  val ready: Actor.Receive = {
    case Start => sender ! "You will be notified when a bus is within range"
      context.become(processing)
      self ! CheckForBus

  }

  val processing: Actor.Receive = {
    case Start =>
      sender ! "Currently in middle of processing. Wait for your text."
    case CheckForBus =>
      getBusTime.map(extractBusesWithinRange).map {
        case Nil => log.info("No buses yet. Scheduling another check in 30 seconds")
          context.system.scheduler.scheduleOnce(30 seconds) {
            self ! CheckForBus
          }
        case bus :: xa => log.info("Bus found. Sending to smsActor")
          // If the first bus is not express, and there's an express in the rest of the list,
          // Send message with both buses. Otherwise just send the head
          BusAlertConfig.EXPRESS_BUS_LINE.flatMap(expLine => xa.find(_.lineName == expLine && bus.lineName != expLine)) match {
            case Some(exBus) =>
              smsSenderActor ! Message(s"${bus.lineName} is ${bus.distance.PresentableDistance} away. " +
                s"There's also a ${exBus.lineName}, ${exBus.distance.PresentableDistance} away")
            case None =>
              smsSenderActor ! Message(s"Bus ${bus.lineName} is ${bus.distance.PresentableDistance} " +
                s"away moving at ${bus.progressRate}")
          }
          context.become(ready)
      }
  }

  private def getBusTime = {
    log.info("Calling Bus Time")
    val pipeLine = sendReceive ~> unmarshal[BusTimeResponse]
    pipeLine {
      Get(busTimeUri)
    }
  }

  def extractBusesWithinRange(busTimeResponse: BusTimeResponse): List[Bus] = {
    log.debug("Extracting Buses from: " + busTimeResponse)
    busTimeResponse.Siri.ServiceDelivery.StopMonitoringDelivery.
      flatMap(_.MonitoredStopVisit).
      map(_.MonitoredVehicleJourney).
      map(b => Bus(b.MonitoredCall.Extensions.Distances, b.PublishedLineName, b.ProgressRate)).
      filter(b => b.distance.DistanceFromCall >= BusAlertConfig.minBusRange && b.distance.DistanceFromCall <= BusAlertConfig.maxBusRange)
  }
}