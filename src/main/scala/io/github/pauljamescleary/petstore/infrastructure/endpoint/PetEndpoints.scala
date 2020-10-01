package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.Validated.Valid
import cats.data._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.domain.authentication.Auth
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}

import domain.{PetAlreadyExistsError, PetNotFoundError}
import domain.pets.{Pet, PetService, PetStatus}
import io.github.pauljamescleary.petstore.domain.users.User
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.authentication._

class PetEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import Pagination._

  /* Parses out status query param which could be multi param */
  implicit val statusQueryParamDecoder: QueryParamDecoder[PetStatus] =
    QueryParamDecoder[String].map(PetStatus.withName)

  /* Relies on the statusQueryParamDecoder implicit, will parse out a possible multi-value query parameter */
  object StatusMatcher extends OptionalMultiQueryParamDecoderMatcher[PetStatus]("status")

  /* Parses out tag query param, which could be multi-value */
  object TagMatcher extends OptionalMultiQueryParamDecoderMatcher[String]("tags")

  implicit val petDecoder: EntityDecoder[F, Pet] = jsonOf[F, Pet]

  private def createPetEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed _ =>
      val action = for {
        pet <- req.request.as[Pet]
        result <- petService.create(pet).value
      } yield result

      action.flatMap {
        case Right(saved) =>
          Ok(saved.asJson)
        case Left(PetAlreadyExistsError(existing)) =>
          Conflict(s"The pet ${existing.name} of category ${existing.category} already exists")
      }
  }

  private def updatePetEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root / LongVar(_) asAuthed _ =>
      val action = for {
        pet <- req.request.as[Pet]
        result <- petService.update(pet).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(PetNotFoundError) => NotFound("The pet was not found")
      }
  }

  private def getPetEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / LongVar(id) asAuthed _ =>
      petService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(PetNotFoundError) => NotFound("The pet was not found")
      }
  }

  private def deletePetEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- petService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def listPetsEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
          offset,
        ) asAuthed _ =>
      for {
        retrieved <- petService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def findPetsByStatusEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / "findByStatus" :? StatusMatcher(Valid(xs)) asAuthed _ =>
      NonEmptyList.fromList(xs) match {
        case None =>
          // User did not specify any statuses
          BadRequest("status parameter not specified")
        case Some(statuses) =>
          // We have a list of valid statuses, find them and return
          for {
            retrieved <- petService.findByStatus(statuses)
            resp <- Ok(retrieved.asJson)
          } yield resp
      }
  }

  private def findPetsByTagEndpoint(petService: PetService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / "findByTags" :? TagMatcher(Valid(xs)) asAuthed _ =>
      NonEmptyList.fromList(xs) match {
        case None =>
          BadRequest("tag parameter not specified")
        case Some(tags) =>
          for {
            retrieved <- petService.findByTag(tags)
            resp <- Ok(retrieved.asJson)
          } yield resp
      }
  }

  def endpoints(
      petService: PetService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createPetEndpoint(petService)
          .orElse(getPetEndpoint(petService))
          .orElse(listPetsEndpoint(petService))
          .orElse(findPetsByStatusEndpoint(petService))
          .orElse(findPetsByTagEndpoint(petService))
      val onlyAdmin =
        deletePetEndpoint(petService).orElse(updatePetEndpoint(petService))

      Auth.allRolesHandler(allRoles)(Auth.adminOnly(onlyAdmin))
    }

    auth.liftService(authEndpoints)
  }
}

object PetEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      petService: PetService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    new PetEndpoints[F, Auth].endpoints(petService, auth)
}
