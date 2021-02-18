package bio.ferlab.clin.etl.fhir.testutils

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.instance.model.api.{IBaseResource, IIdType}
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender
import org.hl7.fhir.r4.model._
import org.slf4j.{Logger, LoggerFactory}

import java.time.{LocalDate, ZoneId}
import java.util.{Collections, Date}

object FhirTestUtils {
  val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("UTC")
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  def loadOrganizations()(implicit fhirClient: IGenericClient): String = {
    val org: Organization = new Organization()
    org.setId("111")
    org.setName("CHU Ste-Justine")
    org.setAlias(Collections.singletonList(new StringType("CHUSJ")))

    val id: IIdType = fhirClient.create().resource(org).execute().getId
    LOGGER.info("Organization created with id : " + id.getIdPart)
    id.getIdPart
  }

  def loadPatients(lastName: String = "Doe", firstName: String = "John", identifier: String = "PT-000001", isActive: Boolean = true, birthDate: LocalDate = LocalDate.of(2000, 12, 21), gender: AdministrativeGender = Enumerations.AdministrativeGender.MALE)(implicit fhirClient: IGenericClient): IIdType = {
    val pt: Patient = new Patient()
    val id1: Resource = pt.setId(identifier)
    pt.addIdentifier()
      .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
      .setValue(identifier)
    pt.setBirthDate(Date.from(birthDate.atStartOfDay(DEFAULT_ZONE_ID).toInstant))
    pt.setActive(isActive)
    pt.addName().setFamily(lastName).addGiven(firstName)
    pt.setIdElement(IdType.of(id1))
    pt.setGender(gender)

    val id = fhirClient.create().resource(pt).execute().getId

    LOGGER.info("Patient created with id : " + id.getIdPart)

    id
  }

  def loadServiceRequest(patientId: String, specimenIds:Seq[String] = Nil)(implicit fhirClient: IGenericClient): String = {
    val sr = new ServiceRequest()
    sr.setSubject(new Reference(s"Patient/$patientId"))
    sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE)
    sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER)
    specimenIds.foreach(spId => sr.addSpecimen(new Reference(s"Specimen/$spId")))
    val id: IIdType = fhirClient.create().resource(sr).execute().getId

    LOGGER.info("ServiceRequest created with id : " + id.getIdPart)
    id.getIdPart

  }

  def loadSpecimen(patientId: String, lab: String = "CHUSJ", submitterId: String = "1", specimenType: String = "BLD", parent: Option[String] = None)(implicit fhirClient: IGenericClient): String = {
    val sp = new Specimen()
    sp.setSubject(new Reference(s"Patient/$patientId"))

    sp.getAccessionIdentifier.setSystem(s"https://cqgc.qc.ca/labs/$lab").setValue(submitterId)

    sp.getType.addCoding()
      .setSystem("http://terminology.hl7.org/CodeSystem/v2-0487")
      .setCode(specimenType)
    parent.foreach { p =>

      sp.addParent(new Reference(s"Specimen/$p"))
    }
    val id: IIdType = fhirClient.create().resource(sp).execute().getId

    LOGGER.info("Specimen created with id : " + id.getIdPart)
    id.getIdPart

  }

  def findById[A <: IBaseResource](id: String, resourceType: Class[A])(implicit fhirClient: IGenericClient): Option[A] = {
    Option(
      fhirClient.read()
        .resource(resourceType)
        .withId(id)
        .execute()
    )
  }

  def clearAll()(implicit fhirClient : IGenericClient): Parameters = {
    val inParams = new Parameters()
    inParams.addParameter().setName("expungeEverything").setValue(new BooleanType(true))
    fhirClient
      .operation()
      .onServer()
      .named("$expunge")
      .withParameters(inParams)
      .execute()
  }

}
