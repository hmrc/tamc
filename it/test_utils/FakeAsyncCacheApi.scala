/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package test_utils

import akka.Done
import com.google.inject.Inject
import net.sf.ehcache.Element
import play.api.cache.AsyncCacheApi

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FakeAsyncCacheApi @Inject() () extends AsyncCacheApi {

  val cache = scala.collection.mutable.Map[String, Element]()

  def set(key: String, value: Any, expiration: Duration): Future[Done] = Future.successful(Done)

  def remove(key: String): Future[Done] = Future.successful(Done)

  def get[T: ClassTag](key: String): Future[Option[T]] = Future.successful(None)

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = orElse

  def removeAll(): Future[Done] = Future.successful(Done)

}

