package ycf.web

import spray.json._

/**
 * Cases classes for converting a SIRI BusTime response to a useful object.
 * Items not currently used are commented out but left in here should they prove useful in the future.
 */

//case class FramedVehicleJourneyRef(DataFrameRef: String, DatedVehicleJourneyRef: String)

//case class VehicleLocation(Longitude: Double, Latitude: Double)

case class Distances(
//                      StopsFromCall: Double,
//                      CallDistanceAlongRoute: Double,
                      DistanceFromCall: Double,
                      PresentableDistance: String
                      )

case class Extensions(Distances: Distances)

case class MonitoredCall(Extensions: Extensions
//                          StopPointRef: String,
//                          VisitNumber: Double,
//                          StopPointName: String
                          )

//case class OnwardCalls()

case class MonitoredVehicleJourney(LineRef: String,
//                                    DirectionRef: String,
//                                    FramedVehicleJourneyRef: FramedVehicleJourneyRef,
//                                    JourneyPatternRef: String,
                                    PublishedLineName: String,
//                                    OperatorRef: String,
//                                    OriginRef: String,
//                                    DestinationRef: String,
//                                    DestinationName: String,
//                                    SituationRef: List[SituationRef],
//                                    Monitored: Boolean,
//                                    VehicleLocation: VehicleLocation,
//                                    Bearing: Double,
                                    ProgressRate: String,
//                                    BlockRef: String,
//                                    VehicleRef: String,
                                    MonitoredCall: MonitoredCall
//                                    OnwardCalls: OnwardCalls
                                    )

case class MonitoredStopVisit(MonitoredVehicleJourney: MonitoredVehicleJourney, RecordedAtTime: String)

case class StopMonitoringDelivery(MonitoredStopVisit: List[MonitoredStopVisit]
//                                   ResponseTimestamp: String,
//                                   ValidUntil: String
                                   )

case class ServiceDelivery(
//                            ResponseTimestamp: String,
                            StopMonitoringDelivery: List[StopMonitoringDelivery]
//                            SituationExchangeDelivery: List[SituationExchangeDelivery]
                            )

case class Siri(ServiceDelivery: ServiceDelivery)

case class BusTimeResponse(Siri: Siri)

object BusTimeResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val distancesFormat = jsonFormat2(Distances)
  implicit val extensionsFormat = jsonFormat1(Extensions)
  implicit val monitoredCallFormat = jsonFormat1(MonitoredCall)
  implicit val monitoredVehicleJourneyFormat = jsonFormat4(MonitoredVehicleJourney)
  implicit val monitoredStopVisitFormat = jsonFormat2(MonitoredStopVisit)
  implicit val stopMonitoringDeliveryFormat = jsonFormat1(StopMonitoringDelivery)
  implicit val serviceDeliveryFormat = jsonFormat1(ServiceDelivery)
  implicit val siriFormat = jsonFormat1(Siri)
  implicit val busTimeResponseFormat = jsonFormat1(BusTimeResponse)
}