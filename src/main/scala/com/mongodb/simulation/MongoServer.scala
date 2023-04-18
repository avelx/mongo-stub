package com.mongodb.simulation

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MongoServer {

  def main(args: Array[String]): Unit = {
    // important to enable HTTP/2 in ActorSystem's config
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "GreeterServer", conf)
    new MongoServer(system).run()
  }
}

class MongoServer(system: ActorSystem[_]) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val ec: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] =
      MongoDbServiceHandler(new MongoDbServiceImpl(system))

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "127.0.0.1", port = 8080)
      //.enableHttps(serverHttpContext)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        println("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }
  //#server


//  private def serverHttpContext: HttpsConnectionContext = {
//    val privateKey =
//      DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem()))
//    val fact = CertificateFactory.getInstance("X.509")
//    val cer = fact.generateCertificate(
//      classOf[GreeterServer].getResourceAsStream("/certs/server1.pem")
//    )
//    val ks = KeyStore.getInstance("PKCS12")
//    ks.load(null)
//    ks.setKeyEntry(
//      "private",
//      privateKey,
//      new Array[Char](0),
//      Array[Certificate](cer)
//    )
//    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
//    keyManagerFactory.init(ks, null)
//    val context = SSLContext.getInstance("TLS")
//    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
//    ConnectionContext.https(context)
//  }
//
//  private def readPrivateKeyPem(): String =
//    Source.fromResource("certs/server1.key").mkString
//  //#server

}
