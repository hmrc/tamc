/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package errors

sealed trait FindRecipientRetrievalError

case object ResourceNotFoundError extends FindRecipientRetrievalError
case object BadRequestError extends FindRecipientRetrievalError
case object ServerError extends FindRecipientRetrievalError
case object ServiceUnavailableError extends FindRecipientRetrievalError
case object ResponseValidationError extends FindRecipientRetrievalError
case object TooManyRequestsError extends FindRecipientRetrievalError
case object TimeOutError extends FindRecipientRetrievalError
case object BadGatewayError extends FindRecipientRetrievalError
case object UnhandledStatusError extends FindRecipientRetrievalError