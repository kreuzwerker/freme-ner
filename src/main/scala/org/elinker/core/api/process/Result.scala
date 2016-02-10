package org.elinker.core.api.process

case class Result(entityType: String, mention: String, beginIndex: Int, endIndex: Int, taIdentRef: Option[String], score: Option[Double])