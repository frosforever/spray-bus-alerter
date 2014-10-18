package ycf.web

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestActorRef}
import org.specs2.mutable.Specification

class BusTimeActorTests extends Specification {

  implicit val testSystem = ActorSystem("testSystem")

  "BusTimeActor" should {
    val actorRef = TestActorRef[BusAlertServiceActor]
    val actor = actorRef.underlyingActor

    "extract buses where one is within range" in {
      val bus = Bus(Distances(BusAlertConfig.minBusRange + 10.5, "5 away"), "Line name", "progressRate")
      val response = createResponse(List(bus))

      actor.extractBusesWithinRange(response) must contain(bus)
    }

    "return Nil when none are present" in {
      val response = createResponse(Nil)
      actor.extractBusesWithinRange(response) must beEmpty
    }

    "return Nil when no buses are in range" in {
      val bus1 = Bus(Distances(BusAlertConfig.minBusRange - 10, "5 away"), "Line name", "progressRate")
      val bus2 = Bus(Distances(BusAlertConfig.maxBusRange + 10, "5 away"), "Line name", "progressRate")
      val response = createResponse(List(bus1, bus2))

      actor.extractBusesWithinRange(response) must beEmpty
    }
  }

  def createResponse(buses: List[Bus]): BusTimeResponse = {
    val monitoredStopVists = buses.map(b =>
      MonitoredStopVisit(MonitoredVehicleJourney("Line Ref", b.lineName, b.progressRate, MonitoredCall(Extensions(b.distance))), "Time Ref"))
    BusTimeResponse(Siri(ServiceDelivery(List(StopMonitoringDelivery(monitoredStopVists)))))
  }

}
