package exd.skynet.client

import cats.ApplicativeError
import cats.effect.Concurrent
import com.comcast.ip4s.{Host, SocketAddress}
import fs2.{RaiseThrowable, Stream}
import fs2.io.net.SocketGroup
import java.net.ConnectException

import cats.effect.kernel.Temporal
import exd.skynet.Protocol.{ClientCommand, ServerCommand}
import exd.skynet.{MessageSocket, Protocol, UserQuit, Username}

import scala.concurrent.duration._

object Client {
  def start[F[_]: Temporal](console: Console[F],
                            socketGroup: SocketGroup[F],
                            address: SocketAddress[Host],
                            desiredUsername: Username): Stream[F, Unit] =
    connect(console, socketGroup, address, desiredUsername).handleErrorWith {
      case _: ConnectException =>
        val retryDelay = 5.seconds
        Stream.eval(
          console.errorln(s"Failed to connect. Retrying in $retryDelay.")) ++
          start(console, socketGroup, address, desiredUsername)
            .delayBy(retryDelay)
      case _: UserQuit => Stream.empty

      case other => Stream.raiseError(other)
    }

  private def connect[F[_]: Concurrent](
      console: Console[F],
      socketGroup: SocketGroup[F],
      address: SocketAddress[Host],
      desiredUsername: Username): Stream[F, Unit] =
    Stream.eval(console.info(s"Connecting to server $address")) ++
      Stream
        .resource(socketGroup.client(address))
        .flatMap { socket =>
          Stream.eval(console.info("🎉 Connected! 🎊")) ++
            Stream
              .eval(
                MessageSocket(socket,
                              ServerCommand.codec,
                              ClientCommand.codec,
                              128))
              .flatMap { messageSocket =>
                Stream.eval(messageSocket.write1(
                  Protocol.ClientCommand.RequestUsername(desiredUsername))) ++
                  processIncoming(messageSocket, console).concurrently(
                    processOutgoing(messageSocket, console))
              }
        }

  private def processIncoming[F[_]](
      messageSocket: MessageSocket[F,
                                   Protocol.ServerCommand,
                                   Protocol.ClientCommand],
      console: Console[F])(
      implicit F: ApplicativeError[F, Throwable]): Stream[F, Unit] =
    messageSocket.read.evalMap {
      case Protocol.ServerCommand.Alert(txt) =>
        console.alert(txt)
      case Protocol.ServerCommand.Message(username, txt) =>
        console.println(s"$username> $txt")
      case Protocol.ServerCommand.SetUsername(username) =>
        console.alert("Assigned username: " + username)
      case Protocol.ServerCommand.Disconnect =>
        F.raiseError[Unit](new UserQuit)
    }

  private def processOutgoing[F[_]: RaiseThrowable](
      messageSocket: MessageSocket[F,
                                   Protocol.ServerCommand,
                                   Protocol.ClientCommand],
      console: Console[F]): Stream[F, Unit] =
    Stream
      .repeatEval(console.readLine("> "))
      .flatMap {
        case Some(txt) => Stream(txt)
        case None      => Stream.raiseError[F](new UserQuit)
      }
      .map(txt => Protocol.ClientCommand.SendMessage(txt))
      .evalMap(messageSocket.write1)
}
