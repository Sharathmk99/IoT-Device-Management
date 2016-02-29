package com.actors

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.actor.Props
import akka.actor.actorRef2Scala
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONString

class RegistrationIdActor(conection: DefaultDB) extends Actor {

  var totalCount = 0
  var resultCount = 0

  def receive = {
    case RegistrationIdQuery =>
      listDocs(conection)
    case WorkerResults =>
      //println("received registration")
      resultCount += 1
      if (totalCount == resultCount) {
        context.system.actorSelection("/user/master") ! WorkerResults
      }
    case _ =>
      println("something else RegistrationIdActor")
  }

  def listDocs(db: DefaultDB) = {
    val collection = db("SensorData")
    val query = BSONDocument()
    val registrationIdList = collection.distinct("registrationId")
    registrationIdList.map { x =>
      {
        totalCount = x.size
        x.foreach { registrationId =>
          {
            //println(registrationId.asInstanceOf[BSONString].value)
            context.system.actorOf(Props(new SensorsIdActor(db))) ! SensorIdQuery(registrationId.asInstanceOf[BSONString].value)
          }
        }
      }
    }
    /*futureList.map { list =>
      {
        if (list.isEmpty)
          println("Empty")
        list.foreach { doc =>
          println(s"found document: ${BSONDocument pretty doc}")
        }
      }
    }*/
  }

}

