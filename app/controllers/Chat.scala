package controllers

import akka.actor._

import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import play.api.mvc.{WebSocket, Controller}

class Chat extends Controller {

  def index = Ok

  def wsEcho = WebSocket.using[String] {
   request => {
     var channel: Option[Concurrent.Channel[String]] = None
     val outEnumerator: Enumerator[String] = Concurrent.unicast(c => channel = Some(c))
     val inIteratee : Iteratee[String, Unit] = Iteratee.foreach[String](receivedString => {
       channel.foreach(_.push(receivedString))
     })
     (inIteratee, outEnumerator)
   }
  }

  object EchoWebSocketActor {
    def props(out: ActorRef) = Props(new EchoWebSocketActor(out))
  }
  class EchoWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String =>
        if (msg == "goodbye") self !PoisonPill
        else out ! s"I received your message: $msg"
    }
  }

  def wsWithActor = WebSocket.acceptWithActor[String, String] { request => out =>
      EchoWebSocketActor.props(out)
  }
}
