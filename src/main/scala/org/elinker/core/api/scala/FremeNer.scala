package org.elinker.core.api.scala

import akka.actor.{ActorSystem, Props}
import edu.stanford.nlp.ie.crf.CRFClassifier
import eu.freme.common.persistence.dao.DatasetSimpleDAO
import org.elinker.core.api.process.Datasets.Dataset
import org.elinker.core.api.process.{DatasetActor, Datasets, EntityLinker}
import spray.routing.RequestContext
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by nilesh on 12/10/15.
 */
class FremeNer(config: Config, datasetDAO: DatasetSimpleDAO) {
  import FremeNer._

  val classifiers = (for((lang, file) <- config.modelFiles)
    yield (lang, CRFClassifier.getClassifierNoExceptions(file))).toMap

  val system = ActorSystem("api")
  private def entityLinker(implicit classifier: CRFClassifier[_], config: Config) = system.actorOf(Props(new EntityLinker(classifier, config.solrURI, getUriTypeMap)))
  private def datasets(implicit config: Config) = system.actorOf(Props(new Datasets(config.solrURI, config.databaseUri, datasetDAO)))

  implicit val timeout = Timeout(5 seconds)
  implicit val configImpl = config

  val domains: Map[String, Set[String]] = {
    Source.fromFile(config.domainsFile).getLines().map{
      case line =>
        val split = line.split(",").filterNot(_.isEmpty)
        (split(0), split.drop(1).toSet)
    }.toMap
  }

  private def getUriTypeMap: Map[String, Set[String]] = {
    val map = mutable.HashMap.empty[String, Set[String]]
    for(line <- Source.fromFile(config.dbpediaInstanceTypesFile).getLines()) {
      val split = line.split(" ")
      val uri = split(0).drop(1).dropRight(1)
      val dbpediaType = split(2).drop(1).dropRight(1)
      if(map contains uri) {
        map += (uri -> (map(uri) + dbpediaType))
      } else {
        map += (uri -> Set(dbpediaType))
      }
    }

    map.toMap
  }

  private def getTypesForDomain(domain: String) = domains(domain)

  def spot(text: String, language: String, outputFormat: String, rdfPrefix: String): String = {
    implicit val classifier = classifiers(language)
    Await.result(entityLinker ? EntityLinker.SpotEntities(text, language, outputFormat, rdfPrefix, classify = false),
      timeout.duration).asInstanceOf[String]
  }

  def spotClassify(text: String, language: String, outputFormat: String, rdfPrefix: String): String = {
    implicit val classifier = classifiers(language)
    Await.result(entityLinker ? EntityLinker.SpotEntities(text, language, outputFormat, rdfPrefix, classify = true),
      timeout.duration).asInstanceOf[String]
  }

  def spotLink(text: String, language: String, dataset: String, outputFormat: String, rdfPrefix: String, numLinks: Int, types: Set[String]): String = {
    implicit val classifier = classifiers(language)
    Await.result(entityLinker ? EntityLinker.SpotLinkEntities(text, language, outputFormat, dataset, rdfPrefix, numLinks, types, classify = false),
      timeout.duration).asInstanceOf[String]
  }

  def spotLink(text: String, language: String, dataset: String, outputFormat: String, rdfPrefix: String, numLinks: Int, domain: String): String = {
    implicit val classifier = classifiers(language)
    val types = domains(domain)
    Await.result(entityLinker ? EntityLinker.SpotLinkEntities(text, language, outputFormat, dataset, rdfPrefix, numLinks, types, classify = false),
      timeout.duration).asInstanceOf[String]
  }

  def spotLinkClassify(text: String, language: String, dataset: String, outputFormat: String, rdfPrefix: String, numLinks: Int, types: Set[String]): String = {
    implicit val classifier = classifiers(language)
    Await.result(entityLinker ? EntityLinker.SpotLinkEntities(text, language, outputFormat, dataset, rdfPrefix, numLinks, types, classify = true),
      timeout.duration).asInstanceOf[String]
  }

  def spotLinkClassify(text: String, language: String, dataset: String, outputFormat: String, rdfPrefix: String, numLinks: Int, domain: String): String = {
    implicit val classifier = classifiers(language)
    val types = domains(domain)
    Await.result(entityLinker ? EntityLinker.SpotLinkEntities(text, language, outputFormat, dataset, rdfPrefix, numLinks, types, classify = true),
      timeout.duration).asInstanceOf[String]
  }

  def addDataset(name: String, dataset: InputType, description: String, format: String, language: String, properties: Array[String]): Dataset = {
    val result = Await.result(dataset match {
      case TextInput(text) =>
        datasets ? Datasets.CreateDataset(name, description, format, Datasets.TextInput(text), language, properties)
      case UrlInput(url) =>
        datasets ? Datasets.CreateDataset(name, description, format, Datasets.UrlInput(url), language, properties)
      case SparqlInput(query, endpoint) =>
        datasets ? Datasets.CreateDataset(name, description, format, Datasets.SparqlInput(query, endpoint), language, properties)
    }, timeout.duration)

    result match {
      case ex: Exception => throw ex
      case dataset: Dataset => dataset
    }
  }

  def updateDataset(name: String, dataset: InputType, description: String, format: String, language: String, properties: Array[String]): Dataset = {
    val result = Await.result(dataset match {
      case TextInput(text) =>
        datasets ? Datasets.UpdateDataset(name, description, format, Datasets.TextInput(text), language, properties)
      case UrlInput(url) =>
        datasets ? Datasets.UpdateDataset(name, description, format, Datasets.UrlInput(url), language, properties)
      case SparqlInput(query, endpoint) =>
        datasets ? Datasets.UpdateDataset(name, description, format, Datasets.SparqlInput(query, endpoint), language, properties)
    }, timeout.duration)

    result.asInstanceOf[Dataset]
  }

  def deleteDataset(name: String): Unit = {
    Await.result(datasets ? DatasetActor.DeleteDataset(name), timeout.duration) match {
      case ex: Exception => throw ex
      case dataset: Dataset => dataset
    }
  }

  def getDataset(name: String): Dataset = {
    Await.result(datasets ? DatasetActor.GetDataset(name), timeout.duration) match {
      case ex: Exception => throw ex
      case dataset: Dataset => dataset
    }
  }

  def getAllDatasets: Array[Dataset] = {
    Await.result(datasets ? DatasetActor.ListDatasets(), timeout.duration) match {
      case ex: Exception => throw ex
      case datasets: List[Dataset] => datasets.toArray
    }
  }
}

object FremeNer {
  abstract class InputType()
  case class TextInput(text: String) extends InputType
  case class UrlInput(url: String) extends InputType
  case class SparqlInput(query: String, endpoint: String) extends InputType
}