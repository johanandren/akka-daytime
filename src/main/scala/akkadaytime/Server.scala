package akkadaytime

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.ByteString

object Server {

  val dayTimeSource: Source[ByteString, NotUsed] =
    Source.single(Unit)
      .map { _ =>
        val string = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())
        ByteString(string, StandardCharsets.US_ASCII)
      }

  val daytimeFlow: Flow[ByteString, ByteString, NotUsed] =
    Flow.fromSinkAndSourceCoupled(Sink.ignore, dayTimeSource)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("daytime")
    implicit val mat = ActorMaterializer()

    // Daytime is TCP/UDP 13, which is protected
    val port = 1313
    // bind to all interfaces
    val host = "0.0.0.0"


    Tcp().bind(host, port).runForeach { incomingConnection =>

      system.log.info("New connection, client address {}:{}",
        incomingConnection.remoteAddress.getHostString,
        incomingConnection.remoteAddress.getPort,
      )

      incomingConnection.handleWith(daytimeFlow)
    }
  }

}
