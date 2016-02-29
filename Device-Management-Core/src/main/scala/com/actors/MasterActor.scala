package com.actors

import akka.actor.{Props, Actor}
import scala.concurrent.duration._
import reactivemongo.api._
import akka.actor.actorRef2Scala

class MasterActor(connection: DefaultDB) extends Actor {
  def receive = {
    case StartWorkers =>
      println("starting workers...import com.actors.RegistrationIdActor")
      startWorkers()
    case WorkerResults =>
      println("stoppign workers...")
      //stopWorkers()
    case _ =>
      println("something else masterActor")
  }

  def startWorkers() = {
    val registrationQuery = context.system.actorOf(Props(new RegistrationIdActor(connection)), "registrationIdQuery")
    registrationQuery.!(RegistrationIdQuery)
  }

  def stopWorkers() = {
    context.stop(self)
  }
}