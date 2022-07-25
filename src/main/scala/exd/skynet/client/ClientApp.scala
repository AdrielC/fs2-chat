package exd.skynet.client

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s.{Host, Port, SocketAddress}
import com.monovore.decline._
import exd.skynet.Username
import fs2.io.net.Network

import scala.Console

object ClientApp extends IOApp {
  private val argsParser: Command[(Username, SocketAddress[Host], Int)] =
    Command("fs2chat-client", "FS2 Chat Client") {
      (
        Opts
          .option[String]("username", "Desired username", "u")
          .map(Username.apply),
        (Opts
          .option[String]("address", "Address of chat server")
          .withDefault("127.0.0.1")
          .mapValidated(p => Host.fromString(p).toValidNel("Invalid IP address")),
        Opts
          .option[Int]("port", "Port of chat server")
          .withDefault(5555)
          .mapValidated(p => Port.fromInt(p).toValidNel("Invalid port number")))
          .mapN(SocketAddress.apply),
        Opts
          .option[Int]("threads", "N threads for socket group")
          .withDefault(1)
      ).tupled
    }

  def run(args: List[String]): IO[ExitCode] =
    argsParser.parse(args) match {
      case Left(help) => IO(System.err.println(help)).as(ExitCode.Error)
      case Right((desiredUsername, address, threads)) =>
        Console[IO]
          .flatMap { console =>
            Network[IO].socketGroup(threadCount = threads).use { socketGroup =>
              Client
                .start[IO](console, socketGroup, address, desiredUsername)
                .compile
                .drain
            }
          }
          .as(ExitCode.Success)
    }
}
