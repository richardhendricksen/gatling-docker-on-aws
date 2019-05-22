package nl.codecontrol.gatling.simulations

import io.gatling.core.Predef._
import nl.codecontrol.gatling.scenarios.BasicScenario.basicScenario
import nl.codecontrol.gatling.config.Config._

import scala.language.postfixOps

class BasicSimulation extends Simulation {

  println("Configuration:")
  println(s"BaseURL: ${baseUrl}")
  println(s"CookieDomain: ${cookieDomain}")
  println(s"Nr concurrent users: ${users}")
  println(s"Max duration: ${maxDuration}")
  println(s"RampUp time: ${rampUpTime}")

  setUp(
    basicScenario.inject(rampUsers(users) during rampUpTime)).maxDuration(maxDuration).protocols(httpProtocol)
    .assertions(
      global.failedRequests.count.is(0)
    )
}
