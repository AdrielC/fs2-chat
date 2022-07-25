package exd.skynet.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s._
import com.monovore.decline._
import fs2.io.net.Network
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ServerApp extends IOApp {
  private val argsParser: Command[(Port, Int)] =
    Command("fs2chat-server", "FS2 Chat Server") {
      (
        Opts
          .option[Int]("port", "Port to bind for connection requests")
          .withDefault(5555)
          .mapValidated(p =>
            Port.fromString(p.toString).toValidNel("Invalid port number")),
        Opts
          .option[Int]("threads", "N threads for socket group")
          .withDefault(1)
      ).tupled
    }

  def run(args: List[String]): IO[ExitCode] =
    argsParser.parse(args) match {
      case Left(help) => IO(System.err.println(help)).as(ExitCode.Error)
      case Right((port, n)) =>
        Network[IO]
          .socketGroup(n)
          .use { socketGroup =>
            Slf4jLogger.create[IO].flatMap { implicit logger =>
              Server.start[IO](socketGroup, port).compile.drain
            }
          }
          .as(ExitCode.Success)
    }
}
