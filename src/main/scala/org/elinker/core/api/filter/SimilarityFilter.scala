package org.elinker.core.api.filter

import com.rockymadden.stringmetric.similarity.JaroWinklerMetric
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.elinker.core.api.process.Result
import scala.collection.JavaConversions._


class SimilarityFilter(solr: HttpSolrClient) {

  val threshold = 0.95

  val SURFACE_FORM_SIMILARITY = "SurfaceFormSimilarity1"


  /**
    * Remove false-positive entities using SOLR index and string similarity
    *
    * @param entity   - entity
    * @param dataset  - SOLR dataset
    * @param language - i18n language
    * @return
    */
  def filterByStringSimilarity(entity: Result, dataset: String, language: String): Boolean = {

    val query = new SolrQuery()
    val taIdentRef = entity.taIdentRef.getOrElse("")
    query.set("q", s"""dataset:"$dataset" AND resource:"$taIdentRef" AND (language:"$language" OR language:"xx")""")
    query.set("sort", "score desc, count desc")

    val response = solr.query("elinker", query)
    response.getResults.filter(x => computeSimilarity(entity.mention, x.get("label").toString)).length > 0
  }

  /**
    * Compute string similarity using JaroWinkler Metric
    *
    * @param surfaceFormA
    * @param surfaceFormB
    * @return
    */
  private def computeSimilarity(surfaceFormA: String, surfaceFormB: String): Boolean = {
    JaroWinklerMetric.compare(surfaceFormA, surfaceFormB).get > threshold
  }

}
