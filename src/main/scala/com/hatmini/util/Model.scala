package com.hatmini.util
import org.mongodb.scala.bson.BsonDocument

object Model {
  case class DataEnvelope(data: BsonDocument, id: String, owner: String)

  case class DataDebitRequest(permissions: Seq[String], dataRecipient: String)

  case class ResourceTypeDataPermission(
      resourceType: String,
      owner: String,
      readers: Seq[String],
      writers: Seq[String]
  )

  case class SuccessfulResourceCreation(
      message: String,
      resourceIdentifier: String
  )

  case class MessageResponse(message: String)

  object ValidPermissions {
    val READ = ("read", "readers")
    val WRITE = ("write", "writers")

    val validPermissions = Set(READ, WRITE)

    val getArrayName: String => Option[String] = rawPermission =>
      validPermissions
        .find(permission => permission._1 == rawPermission)
        .map(permission => permission._2)
  }
}
