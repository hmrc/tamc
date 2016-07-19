/*
 * Copyright 2016 HM Revenue & Customs
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

package config

import uk.gov.hmrc.play.config.ServicesConfig
import play.api.Play.configuration
import play.api.Play.current

trait ApplicationConfig {
  val EMAIL_GDS_TEMPLATE_ID: String
  val EMAIL_PTA_TEMPLATE_ID: String
  val EMAIL_UPDATE_CANCEL_TEMPLATE_ID: String
  val EMAIL_UPDATE_REJECT_TEMPLATE_ID: String
  val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_TEMPLATE_ID: String
  val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_TEMPLATE_ID: String
  val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR: String
  val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR: String
  val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR: String
  val EMAIL_APPLY_CURRENT_TAXYEAR_TEMPLATE_ID: String
  val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID:String
  val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID: String
  val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR: String
  val ROLE_TRANSFEROR: String
  val ROLE_RECIPIENT: String
  val REASON_CANCEL: String
  val REASON_REJECT: String
  val REASON_DIVORCE: String
  val START_TAX_YEAR: Int
  val START_DATE: String
  val END_DATE: String
  val START_DATE_CY: String
  val END_DATE_CY: String
  
  val EMAIL_UPDATE_CANCEL_WELSH_TEMPLATE_ID: String
  val EMAIL_UPDATE_REJECT_WELSH_TEMPLATE_ID: String
  val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_WELSH_TEMPLATE_ID: String
  val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_WELSH_TEMPLATE_ID: String
  val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR_WELSH: String
  val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR_WELSH: String
  val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR_WELSH: String
  val EMAIL_APPLY_CURRENT_TAXYEAR_WELSH_TEMPLATE_ID: String
  val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID: String
  val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID: String 
  val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR_WELSH: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {
  
  override val EMAIL_GDS_TEMPLATE_ID = "tamc_confirmation_template_id"
  override val EMAIL_PTA_TEMPLATE_ID = "tamc_confirmation_pta"
  
  override val EMAIL_UPDATE_CANCEL_TEMPLATE_ID = "tamc_update_cancel"
  override val EMAIL_UPDATE_REJECT_TEMPLATE_ID = "tamc_update_reject"
  override val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_TEMPLATE_ID = "tamc_update_divorce_transferor_boy"
  override val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_TEMPLATE_ID = "tamc_update_divorce_recipient_eoy"
  override val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR = "tamc_transferor_divorce_previous_yr"
  override val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR = "tamc_recipient_divorce_previous_yr"
  override val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR = "tamc_recipient_rejects_retro_yr"
  override val EMAIL_APPLY_CURRENT_TAXYEAR_TEMPLATE_ID = "tamc_current_year"
  override val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID = "tamc_retro_year"
  override val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID = "tamc_current_retro_year"
  override val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR = "tamc_transferor_divorce_current_yr"
  
  override val EMAIL_UPDATE_CANCEL_WELSH_TEMPLATE_ID = "tamc_update_cancel_cy"  
  override val EMAIL_UPDATE_REJECT_WELSH_TEMPLATE_ID = "tamc_update_reject_cy"
  override val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_WELSH_TEMPLATE_ID = "tamc_update_divorce_transferor_boy_cy"
  override val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_WELSH_TEMPLATE_ID = "tamc_update_divorce_recipient_eoy_cy"
  override val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR_WELSH = "tamc_transferor_divorce_previous_yr_cy"
  override val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR_WELSH = "tamc_recipient_divorce_previous_yr_cy"
  override val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR_WELSH = "tamc_recipient_rejects_retro_yr_cy"  
  override val EMAIL_APPLY_CURRENT_TAXYEAR_WELSH_TEMPLATE_ID = "tamc_current_year_cy"
  override val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID = "tamc_retro_year_cy"
  override val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID = "tamc_current_retro_year_cy"
  override val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR_WELSH = "tamc_transferor_divorce_current_yr_cy"
  
  override val ROLE_TRANSFEROR = "Transferor"
  override val ROLE_RECIPIENT = "Recipient"

  override val REASON_CANCEL = "Cancelled by Transferor"
  override val REASON_REJECT = "Rejected by Recipient"
  override val REASON_DIVORCE = "Divorce/Separation"
  
  override val START_DATE = "6 April "
  override val END_DATE = "5 April "

  val START_DATE_CY = "6 Ebrill"
  val END_DATE_CY = "5 Ebrill"
  
  override val START_TAX_YEAR = configuration.getInt("ma-start-tax-year").getOrElse(2015)
}
