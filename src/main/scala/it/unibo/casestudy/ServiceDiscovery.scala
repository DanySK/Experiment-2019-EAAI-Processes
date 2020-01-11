package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}
import Services._

class ServiceDiscovery extends AggregateProgram with StandardSensors with Gradients with BlockG with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with BlockS with BlockC {

  import Spawn._

  lazy val providedServices: Set[ProvidedService] =
    randomGenerator()
      .shuffle(services)
      .take(randomGenerator().nextInt(services.size))
      .map(s => ProvidedService(s)(numInstances = 1)
    )

  var allocatedServices: Set[ProvidedService] = Set.empty

  def availableServices: Set[ProvidedService] = providedServices.filter(_.freeInstances>0)

  def task: Option[Task] =
    if(node.has("task"))
      Some(node.get[Task]("task"))
    else
      None

  override def main(): Any = {
    node.put("providedServices", providedServices)
    node.put("hasTask", task.isDefined)
    node.put("t_time", currentTime())
    node.put("t_timestamp", timestamp())

    if(node.get[Boolean]("algorithm")){
      processBasedServiceDiscovery()
    } else {
      classicServiceDiscovery()
    }

  }

  def classicServiceDiscovery() = {
    val hasTask = task.isDefined
    val g = classicGradient(hasTask)
    val gHops = G[Int](hasTask, 0, _+1, nbrRange _)
    node.put("gradient", f"$g%2.1f")

    var taskChanged = false
    var hops: Map[ID,Int] = Map.empty

    val taskRequest = rep[Option[TaskRequest]](None) { tr =>
        val theTask = task.map(t => TaskRequest(mid, t)(allocation = Map.empty))
        taskChanged = theTask != tr
        val localTaskRequest = if(tr==theTask) tr else theTask
        val receivedRequest = bcast(hasTask, localTaskRequest)
        node.put("receivedRequest", receivedRequest.isDefined)
        node.put("request", receivedRequest)
        if(receivedRequest.isEmpty && node.has("requestBy")) node.remove("requestBy")
        else if(receivedRequest.isDefined) node.put("requestBy", receivedRequest.get.requestor%20)

        val offeredService: Option[Service] = receivedRequest.flatMap { request =>
          request.allocation.find(_._2 == mid).map(_._1) // keep current allocation
            .orElse[Service] { // or offer an available service
              request.missingServices.collectFirst { case s if availableServices.exists(_.service == s) => s }
            }
        }
        node.put("offersService", offeredService.isDefined)
        node.put("offeredService", offeredService)

        allocatedServices = Set.empty
        offeredService.foreach { s =>
          allocatedServices += ProvidedService(s)()
        }

        val offers = C[Double, Map[ID, Service]](g, _ ++ _, offeredService.map(s => Map(mid -> s)).getOrElse(Map.empty), Map.empty)
        hops = C[Double,Map[ID,Int]](g, _++_, offeredService.map(s => Map(mid -> gHops)).getOrElse(Map.empty), Map.empty)
        localTaskRequest.map(t => t.copy()(allocation = chooseFromOffers(t, offers)))
    }

    // Time a device is trying to satisfy a task
    val tryFor: Long = branch(!taskChanged && taskRequest.exists(_.missingServices.nonEmpty) ){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Time a device has a task satisfied
    val keepFor: Long = branch(taskRequest.isDefined && taskRequest.get.missingServices.isEmpty){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Condition for task removal (accomplishment)
    val accomplished = keepFor > node.get[Number]("taskPropagationTime").longValue
    val giveUp = tryFor > node.get[Number]("taskConclusionTime").longValue
    if((accomplished || giveUp) && node.has("task")){
      node.remove("task")
      val latency: Int = (timestamp()-node.get[Long]("taskTime")).toInt
      node.put("taskLatency", if(node.has("taskLatency")) node.get[Int]("taskLatency")+latency else latency)
      val numHops: Int = taskRequest.get.allocation.values.map(hops(_)).sum
      val cloudHops: Int = taskRequest.get.missingServices.map(_ => node.get[Int]("cloudcost")).sum
      val totalHops = numHops + cloudHops
      node.put("taskHops", if(node.has("taskHops")) node.get[Int]("taskHops") + totalHops else totalHops)
      if(accomplished) {
        node.put("completedTasks", if(node.has("completedTasks")) node.get[Int]("completedTasks")+1 else 1)
      }
      if(giveUp) {
        node.put("giveupTasks", if(node.has("giveupTasks")) node.get[Int]("giveupTasks")+1 else 1)
      }
    }
  }

  def chooseFromOffers(req: TaskRequest, offers: Map[ID,Service]): Map[Service,ID] = {
    var servicesToAlloc = req.missingServices
    var newAllocations = Map[Service,ID]()
    for(offer <- offers){
      if(servicesToAlloc.contains(offer._2)){
        newAllocations += offer.swap
        servicesToAlloc -= offer._2
      }
    }

    // keep current allocations if still provided, and add new allocations
    req.allocation.filter(curr => offers.contains(curr._2)) ++ newAllocations
  }

  def processBasedServiceDiscovery() = {

  }

  // Other stuff

  def bcast[V](source: Boolean, field: V): V =
    G[V](source, field, v => v, nbrRange _)

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)
}