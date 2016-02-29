package com.main;
import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import scala.concurrent.duration._
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import reactivemongo.api.commands.WriteResult
import com.actors._
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.http.service.HttpServiceActor

object HelloAkkaScala extends App {

  // Create the 'helloakka' actor system
  implicit val system = ActorSystem("helloakka")
  val connection = connect()
  val masterActor = system.actorOf(Props(new MasterActor(connection)), "master")
  masterActor ! StartWorkers
  //system.scheduler.schedule(0.seconds, 1.second, greeter, WhoToGreet("akka"))
  def connect(): DefaultDB = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    val db = connection("DeviceManagement")
    db
  }
  val httpServiceActor = system.actorOf(Props(new HttpServiceActor(connection)), "http-service")
  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(httpServiceActor, interface = "localhost", port = 8080)
}
