
import spray.http.{MediaTypes, HttpHeaders, HttpMethods}

import scala.concurrent.{Future, Await}
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._
import spray.http._


case class AddressComponent(long_name : String , short_name : String , types: List[String])
case class Location(lat : Double , lng : Double)
case class ViewPort(northeast: Location , southwest : Location)
case class Geometry(location : Location , location_type:String , viewport: ViewPort)
case class EachResult(
                       address_components : List[AddressComponent]
                       , formatted_address : String
                       , geometry : Geometry
                       , partial_match: Boolean
                       , types : List[String])
case class GoogleApiResult[T](status: String , results:List[T])

object AddressProtocol extends DefaultJsonProtocol {
  implicit val addressFormat     = jsonFormat3(AddressComponent)
  implicit val locFormat      = jsonFormat2(Location)
  implicit val viewPortFormat = jsonFormat2(ViewPort)
  implicit val geomFormat     = jsonFormat3(Geometry)
  implicit val ResFormat  = jsonFormat5(EachResult)
  implicit def GoogleApiFormat[T: JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
}





object Main extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below
  val log = Logging(system, getClass)

  import AddressProtocol._
  import  SprayJsonSupport._
  val pipeline = sendReceive ~> unmarshal[GoogleApiResult[EachResult]]
  val pipe: HttpRequest => Future[HttpResponse] = sendReceive


  val ClientId = "GEJ3PUD4EEC40FOXNSNFELY5GJSHYJGSLA43W5H1E1FQCRFU"
  val ClientSerect = "PONM1ZMGNDCNTQ4RAAY5KJUKQU5HHCT4PEJTO4GD3NIZ5BXB"
  findAResterant("Pizza","Toyko Japan")

  def findAResterant(mealType:String,location:String) = {
    val LatAndLongLocation = getGeoLocation("Toyko Japan")
    val lat = LatAndLongLocation._1.toString
    val long =  LatAndLongLocation._2.toString
    val urlForFourSquareWithLatAndLong = s"https://api.foursquare.com/v2/venues/search?client_id=$ClientId&client_secret=$ClientSerect&v=20130815&ll=$lat,$long&query=$mealType"
    println("this is our url " + urlForFourSquareWithLatAndLong)

    val responseFutureFourSquare = pipe{Get(urlForFourSquareWithLatAndLong)}
    responseFutureFourSquare onComplete {
        case Success(response) =>  println("this is our entity" + response.entity)

        case Failure(error) => println(error.getMessage)
      }
  }

  def getGeoLocation(location:String):(Double,Double) = {
   val googleApiKey = "AIzaSyCMUmj4Im-EXOahHW611eRkcKrtyysbkNg"
   val editString =  location.replace(" " , "+")
   val goggleMapsApiurl = (s"https://maps.googleapis.com/maps/api/geocode/json?address=$editString,+CA&key=$googleApiKey")
    val responseFutureGeo = pipeline {Get(goggleMapsApiurl)}
    responseFutureGeo onComplete {
      case Success(GoogleApiResult(status , EachResult(address_components , formatted_address , geometry , partial_match , types) :: _)) =>
        log.info("This is the latitude of my city " + geometry.location.lat + " the long " + geometry.location.lng)
      case Success(somethingUnexpected) =>
        log.warning("The Google API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
        system.shutdown()
    case Failure(error) =>
    log.error(error, "Couldn't get location of geo")
      system.shutdown()
  }
   val notAfuture =  Await.result(responseFutureGeo,10 seconds)
    val LatAndLong = (notAfuture.results.head.geometry.location.lat,notAfuture.results.head.geometry.location.lng)
    LatAndLong
  }



  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}
