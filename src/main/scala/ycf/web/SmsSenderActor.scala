package ycf.web

import akka.actor.{Stash, Actor, ActorLogging}
import spray.client.pipelining._
import spray.http.FormData
import scala.concurrent.duration._

import scala.language.postfixOps
import scala.util.{Failure, Success}

case object Ready
case class Message(msg: String)

/**
 * Converts a [[Bus]] into a message that's then
 * sent using [[http://textbelt.com Textbelt]].
 */
class SmsSenderActor extends Actor with ActorLogging {
  import context.dispatcher

  val smsEndPoint = "http://textbelt.com/text"
  val number = BusAlertConfig.SMS_NUMBER

  var messageWhileWaiting: Option[String] = None

  override def receive = normal

  val normal: Actor.Receive = {
    case Message(msg) => sendMessage(msg)
  }

  val waiting: Actor.Receive = {
    case Message(msg) =>
      messageWhileWaiting = Some(msg) // Just keeps last message.
      log.info("Message received while waiting: " + msg)
    case Ready =>
      context.become(normal)
      messageWhileWaiting.foreach(msg => self ! Message("Message received during wait time: " + msg))
      messageWhileWaiting = None
  }

  val pipeLine = sendReceive ~> unmarshal[String]

  /**
   * Sends the message with logging.
   * Changes context to waiting so that do not exceeded message send limit of 1 / min.
   * Sends [[Ready]] message after 1 minute to resume normal processing
   * @param message
   * @return
   */
  def sendMessage(message: String) = {
    pipeLine {
      val formData = FormData(Seq("number" -> number, "message" -> message))
      Post(smsEndPoint, formData)
    }.onComplete {
      // TextBelt always replies with 200 even if failed to send SMS.
      // Need to process the response to get notification of failure
      case Success(res) if res.contains("false") => log.error("Sms failed to send correctly: " + res)
      case Success(res) if res.contains("true") => log.debug("Sms sent successfully")
      case Failure(e) => log.error(e, "Failed calling SMS")
      case somethingElse => log.error("Unexpected response from sms send: " + somethingElse)
    }
    context.become(waiting)
    context.system.scheduler.scheduleOnce(60 seconds) {
      self ! Ready
    }
  }
}