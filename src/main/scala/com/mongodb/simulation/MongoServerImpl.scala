package com.mongodb.simulation


import scala.concurrent.Future

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.MergeHub
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

class MongoDbServiceImpl(system: ActorSystem[_]) extends MongoDbService {
  private implicit val sys: ActorSystem[_] = system

  //#service-request-reply
  val (inboundHub: Sink[MsgHeader, NotUsed], outboundHub: Source[MsgHeader, NotUsed]) =
    MergeHub.source[MsgHeader]
      .map(request => MsgHeader(requestID = 0, responseTo = 1, opCode = 3))
      .toMat(BroadcastHub.sink[MsgHeader])(Keep.both)
      .run()
  //#service-request-reply

  override def sayHello(request: MsgHeader): Future[MsgHeader] = {
    Future.successful(MsgHeader(requestID = 0, responseTo = 1, opCode = 3))
  }

}
