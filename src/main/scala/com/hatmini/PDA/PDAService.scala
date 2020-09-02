package com.hatmini.PDA
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{
  fromRegistries,
  fromProviders
}
import scala.concurrent.Future
import org.mongodb.scala.SingleObservable
import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Updates._
import java.{util => ju}
import com.hatmini.util.Model._
import shapeless.Succ
import cats.data.Validated.Valid
import org.mongodb.scala.bson.codecs.Macros
import akka.http.javadsl.model.ws.Message

class PDAService {

  lazy val mongoClient: MongoClient = MongoClient()

  lazy val codecRegisteries =
    fromRegistries(
      fromProviders(classOf[DataEnvelope], classOf[ResourceTypeDataPermission]),
      DEFAULT_CODEC_REGISTRY
    )

  def createResource(
      resourceType: String,
      databody: BsonDocument,
      owner: String
  ): Future[SuccessfulResourceCreation] = {
    val uuid = ju.UUID.randomUUID().toString()
    val dataEnvelope = DataEnvelope(databody, uuid, owner)
    mongoClient
      .getDatabase(owner)
      .withCodecRegistry(codecRegisteries)
      .getCollection[DataEnvelope](resourceType)
      .insertOne(dataEnvelope)
      .toFuture()
      .map(_ =>
        SuccessfulResourceCreation(
          s"Successfully created resource in ${resourceType} repository",
          uuid
        )
      )
  }

  def getResources(
      resourceType: String,
      owner: String
  ): Future[Seq[DataEnvelope]] = {
    mongoClient
      .getDatabase(owner)
      .withCodecRegistry(codecRegisteries)
      .getCollection[DataEnvelope](resourceType)
      .find()
      .toFuture()
  }

  def dataDebit(
      resourceType: String,
      owner: String,
      dataRecipient: String,
      permissions: Seq[String]
  ): Future[MessageResponse] = {
    val extractedPermissions = permissions.flatMap(permission =>
      ValidPermissions.validPermissions.find(validPermission =>
        validPermission._1 == permission
      )
    )
    val permissionsCollection = mongoClient
      .getDatabase("permissions")
      .withCodecRegistry(codecRegisteries)
      .getCollection[ResourceTypeDataPermission](owner)

    permissionsCollection
      .find(Filters.eq("resourceType", resourceType))
      .toFuture()
      .map(_.headOption)
      .flatMap(permissionsEntry =>
        permissionsEntry match {
          case Some(permissions) => {
            val permissionSetters = extractedPermissions
              .map(permission => addToSet(permission._2, dataRecipient))
            permissionsCollection
              .updateOne(
                Filters.eq("resourceType", resourceType),
                combine(permissionSetters: _*)
              )
              .toFuture()
              .map(_ => MessageResponse("Successfully created data debit"))
          }
          case None => {
            val readPermissions: List[String] = extractedPermissions
              .find(p => p == ValidPermissions.READ)
              .fold(List.empty[String])(permission => List(dataRecipient))
            val writePermissions: List[String] = extractedPermissions
              .find(p => p == ValidPermissions.READ)
              .fold(List.empty[String])(permission => List(dataRecipient))

            permissionsCollection
              .insertOne(
                ResourceTypeDataPermission(
                  resourceType,
                  owner,
                  readPermissions,
                  writePermissions
                )
              )
              .toFuture()
              .map(_ => MessageResponse("Successfully created data debit"))
          }
        }
      )
  }

  def getResourceAsNonOwner(
      resourceType: String,
      owner: String,
      requestor: String
  ): Future[Seq[DataEnvelope]] = {
    val hasPermissions = mongoClient
      .getDatabase("permissions")
      .withCodecRegistry(codecRegisteries)
      .getCollection[ResourceTypeDataPermission](owner)
      .find(Filters.eq("resourceType", resourceType))
      .toFuture()
      .map(
        _.headOption.fold(false)(permission =>
          permission.readers.contains(requestor)
        )
      )
    hasPermissions.flatMap(permission => {
      if (!permission) {
        Future(Seq.empty[DataEnvelope])
      } else {
        getResources(resourceType, owner)
      }
    })
  }

  def createResourceAsNonOwner(
      resourceType: String,
      owner: String,
      requestor: String,
      databody: BsonDocument
  ): Future[Either[MessageResponse, SuccessfulResourceCreation]] = {
    val hasPermissions = mongoClient
      .getDatabase("permissions")
      .withCodecRegistry(codecRegisteries)
      .getCollection[ResourceTypeDataPermission](owner)
      .find(Filters.eq("resourceType", resourceType))
      .toFuture()
      .map(
        _.headOption.fold(false)(permission =>
          permission.writers.contains(requestor)
        )
      )

    hasPermissions.flatMap(permission => {
      if (!permission) {
        Future(
          Left(MessageResponse("Insuffienct permissions to create resource"))
        )
      } else {
        createResource(resourceType, databody, owner).map(response =>
          Right(response)
        )
      }
    })
  }
}
