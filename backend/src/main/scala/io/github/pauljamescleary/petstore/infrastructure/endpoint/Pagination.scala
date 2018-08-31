package io.github.pauljamescleary.petstore.infrastructure.endpoint

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object Pagination {

  /* Necessary for decoding query parameters */
  import QueryParamDecoder._

  /* Parses out the offset and page size params */
  object PageSizeMatcher extends QueryParamDecoderMatcher[Int]("pageSize")
  object OffsetMatcher extends QueryParamDecoderMatcher[Int]("offset")
}
