package bio.ferlab.clin.etl.task.fileimport.validation

import bio.ferlab.clin.etl.fhir.IClinFhirClient
import bio.ferlab.clin.etl.fhir.IClinFhirClient.opt
import bio.ferlab.clin.etl.task.fileimport.model.{Analysis, TServiceRequest}
import cats.data.ValidatedNel
import cats.implicits._
import scala.collection.JavaConverters._
import org.hl7.fhir.r4.model.IdType

object ServiceRequestValidation {

  def validateServiceRequest(a: Analysis)(implicit client: IClinFhirClient): ValidatedNel[String, TServiceRequest] = {
    val fhirServiceRequest = opt(client.getServiceRequestById(new IdType(a.clinServiceRequestId)))
    fhirServiceRequest match {
      case None => s"ServiceRequest id=${a.clinServiceRequestId} does not exist".invalidNel[TServiceRequest]
      case Some(fsr) => TServiceRequest(fsr).validNel[String]
    }
  }

}
