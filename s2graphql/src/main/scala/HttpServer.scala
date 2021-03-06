/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.s2graph

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import Console._
import scala.concurrent.Await
import scala.language.postfixOps

object Server extends App {

  implicit val actorSystem = ActorSystem("s2graphql-server")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher
  import scala.concurrent.duration._

  println("Starting GRAPHQL server...")

  val route: Route =
    (post & path("graphql")) {
      entity(as[spray.json.JsValue])(GraphQLServer.endpoint)
    } ~ {
      getFromResource("graphiql.html")
    }

  val port = sys.props.get("http.port").fold(8000)(_.toInt)
  Http().bindAndHandle(route, "0.0.0.0", port)


  def shutdown(): Unit = {
    println("Terminating...")
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10 seconds)

    println("Terminated.")
  }
}
