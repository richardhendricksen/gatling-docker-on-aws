package nl.codecontrol.gatling.config

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

object Config {
  // params
  val baseUrl: String = sys.env.getOrElse("GATLING_BASEURL", "http://computer-database.gatling.io").toString
  val users: Int = sys.env.getOrElse("GATLING_USERS", "1").toInt
  val maxDuration: FiniteDuration = sys.env.getOrElse("GATLING_MAX_DURATION", "1").toInt minutes
  val rampUpTime: FiniteDuration = sys.env.getOrElse("GATLING_RAMPUP_TIME", "10").toInt seconds

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

}
