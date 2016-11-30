package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxDownloadFile;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.dtos.JaxMahnung;
import ch.dvbern.ebegu.api.util.RestUtil;
import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.enums.GeneratedDokumentTyp;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.MergeDocException;
import ch.dvbern.ebegu.services.*;
import ch.dvbern.lib.cdipersistence.Persistence;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.Validate;

import javax.activation.MimeTypeParseException;
import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * REST Resource fuer Institution
 */
@Path("blobs/temp")
@Stateless
@Api
public class DownloadResource {


	@Inject
	private DownloadFileService downloadFileService;

	@Inject
	private JaxBConverter converter;

	@Inject
	private DokumentService dokumentService;

	@Inject
	private VorlageService vorlageService;

	@Inject
	private GesuchService gesuchService;

	@Inject
	private BetreuungService betreuungService;

	@Inject
	private GeneratedDokumentService generatedDokumentService;

	@Inject
	private Persistence<AbstractEntity> persistence;


	@GET
	@Path("blobdata/{accessToken}/{filename}")
	//mimetyp wird in buildDownloadResponse erraten
	public Response downloadByAccessToken(
		@PathParam("accessToken") String blobAccessTokenParam,
		@PathParam("filename") String filename,
		@MatrixParam("attachment") @DefaultValue("false") boolean attachment,
		@Context HttpServletRequest request) {

		String ip = getIP(request);


		DownloadFile downloadFile = downloadFileService.getDownloadFileByAccessToken(blobAccessTokenParam);

		if (downloadFile == null) {
			return Response.status(Response.Status.FORBIDDEN).entity("Ung&uuml;ltige Anfrage f&uuml;r download").build();
		}

		if (!downloadFile.getIp().equals(ip)) {
			return Response.status(Response.Status.FORBIDDEN).entity("Keine Berechtigung f&uuml;r download").build();
		}

		try {
			return RestUtil.buildDownloadResponse(downloadFile, attachment);
		} catch (IOException e) {
			return Response.status(Response.Status.NOT_FOUND).entity("Dokument kann nicht gelesen werden").build();
		}

	}

	@Nonnull
	@GET
	@Path("/{dokumentId}/dokument")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.WILDCARD)
	public Response getDokumentAccessTokenDokument(
		@Nonnull @Valid @PathParam("dokumentId") JaxId jaxId,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException {

		String ip = getIP(request);

		Validate.notNull(jaxId.getId());
		String id = converter.toEntityId(jaxId);

		final File dokument = dokumentService.findDokument(id)
			.orElseThrow(() -> new EbeguEntityNotFoundException("getDokumentAccessTokenDokument", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, id));


		return getFileDownloadResponse(uriInfo, ip, dokument);
	}

	@Nonnull
	@GET
	@Path("/{dokumentId}/vorlage")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.WILDCARD)
	public Response getDokumentAccessTokenVorlage(
		@Nonnull @Valid @PathParam("dokumentId") JaxId jaxId,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException {

		String ip = getIP(request);

		Validate.notNull(jaxId.getId());
		String id = converter.toEntityId(jaxId);

		final File dokument = vorlageService.findVorlage(id)
			.orElseThrow(() -> new EbeguEntityNotFoundException("getDokumentAccessTokenVorlage", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, id));


		return getFileDownloadResponse(uriInfo, ip, dokument);
	}

	/**
	 * Methode fuer alle GeneratedDokumentTyp. Hier wird es allgemein mit den Daten vom Gesuch gearbeitet.
	 * Alle anderen Vorlagen, die andere Daten brauchen, muessen ihre eigene Methode haben. So wie bei VERFUEGUNG
	 *
	 * @param jaxGesuchId gesuch ID
	 * @param dokumentTyp Typ der Vorlage
	 * @param request     request
	 * @param uriInfo     uri
	 * @return ein Response mit dem GeneratedDokument
	 * @throws EbeguEntityNotFoundException
	 * @throws MergeDocException
	 */
	@Nonnull
	@GET
	@Path("/{gesuchid}/{dokumentTyp}/{forceCreation}/generated")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.WILDCARD)
	public Response getDokumentAccessTokenGeneratedDokument(
		@Nonnull @Valid @PathParam("gesuchid") JaxId jaxGesuchId,
		@Nonnull @Valid @PathParam("dokumentTyp") GeneratedDokumentTyp dokumentTyp,
		@Nonnull @Valid @PathParam("forceCreation") Boolean forceCreation,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException, MergeDocException, MimeTypeParseException {

		Validate.notNull(jaxGesuchId.getId());
		Validate.notNull(dokumentTyp);
		String ip = getIP(request);

		final Optional<Gesuch> gesuch = gesuchService.findGesuch(converter.toEntityId(jaxGesuchId));
		if (gesuch.isPresent()) {
			GeneratedDokument generatedDokument = generatedDokumentService.getDokumentAccessTokenGeneratedDokument(gesuch.get(), dokumentTyp, forceCreation);
			if (generatedDokument == null) {
				return Response.noContent().build();
			}
			return getFileDownloadResponse(uriInfo, ip, generatedDokument);
		}
		throw new EbeguEntityNotFoundException("getDokumentAccessTokenGeneratedDokument",
			ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "GesuchId invalid: " + jaxGesuchId.getId());
	}

	@Nonnull
	@POST
	@Path("/{gesuchid}/{betreuungId}/{dokumentTyp}/{forceCreation}/generated")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.WILDCARD)
	public Response getVerfuegungDokumentAccessTokenGeneratedDokument(
		@Nonnull @Valid @PathParam("gesuchid") JaxId jaxGesuchId,
		@Nonnull @Valid @PathParam("betreuungId") JaxId jaxBetreuungId,
		@Nonnull @Valid @PathParam("forceCreation") Boolean forceCreation,
		@Nonnull @NotNull @Valid String manuelleBemerkungen,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException, MergeDocException,
		IOException, MimeTypeParseException {

		Validate.notNull(jaxGesuchId.getId());
		Validate.notNull(jaxBetreuungId.getId());
		String ip = getIP(request);

		final Optional<Gesuch> gesuchOptional = gesuchService.findGesuch(converter.toEntityId(jaxGesuchId));
		if (gesuchOptional.isPresent()) {
			Betreuung betreuung = gesuchOptional.get().extractBetreuungById(jaxBetreuungId.getId());

			// Wir verwenden das Gesuch nur zur Berechnung und wollen nicht speichern, darum das Gesuch detachen
			loadRelationsAndDetach(gesuchOptional.get());

			GeneratedDokument persistedDokument = generatedDokumentService
				.getVerfuegungDokumentAccessTokenGeneratedDokument(gesuchOptional.get(), betreuung, manuelleBemerkungen, forceCreation);
			return getFileDownloadResponse(uriInfo, ip, persistedDokument);

		}
		throw new EbeguEntityNotFoundException("getVerfuegungDokumentAccessTokenGeneratedDokument",
			ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "GesuchId not found: " + jaxGesuchId.getId());
	}

	@Nonnull
	@PUT
	@Path("/MAHNUNG/{forceCreation}/generated")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.WILDCARD)
	public Response getMahnungDokumentAccessTokenGeneratedDokument(
		@Nonnull @NotNull @Valid JaxMahnung jaxMahnung,
		@Nonnull @Valid @PathParam("forceCreation") Boolean forceCreation,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException,
		IOException, MimeTypeParseException, MergeDocException {

		Validate.notNull(jaxMahnung);
		String ip = getIP(request);

		Mahnung mahnung = converter.mahnungToEntity(jaxMahnung, new Mahnung());

		GeneratedDokument persistedDokument = generatedDokumentService
			.getMahnungDokumentAccessTokenGeneratedDokument(mahnung, forceCreation);

		return getFileDownloadResponse(uriInfo, ip, persistedDokument);

	}

	@Nonnull
	@GET
	@Path("/{betreuungId}/NICHTEINTRETEN/{forceCreation}/generated")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.WILDCARD)
	public Response getNichteintretenDokumentAccessTokenGeneratedDokument(
		@Nonnull @Valid @PathParam("betreuungId") JaxId jaxBetreuungId,
		@Nonnull @Valid @PathParam("forceCreation") Boolean forceCreation,
		@Context HttpServletRequest request, @Context UriInfo uriInfo) throws EbeguEntityNotFoundException,
		IOException, MimeTypeParseException, MergeDocException {

		Validate.notNull(jaxBetreuungId);
		String ip = getIP(request);

		Optional<Betreuung> betreuung = betreuungService.findBetreuung(jaxBetreuungId.getId());

		GeneratedDokument persistedDokument = generatedDokumentService
			.getNichteintretenDokumentAccessTokenGeneratedDokument(betreuung.get(), forceCreation);

		return getFileDownloadResponse(uriInfo, ip, persistedDokument);

	}

	private Response getFileDownloadResponse(UriInfo uriInfo, String ip, File file) {
		final DownloadFile downloadFile = downloadFileService.create(file, ip);

		URI uri = uriInfo.getBaseUriBuilder()
			.path(DownloadResource.class)
			.path("/" + downloadFile.getId())
			.build();

		JaxDownloadFile jaxDownloadFile = converter.downloadFileToJAX(downloadFile);

		return Response.created(uri).entity(jaxDownloadFile).build();
	}

	private String getIP(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}

	/**
	 * Hack, welcher das Gesuch detached, damit es auf keinen Fall gespeichert wird. Vorher muessen die Lazy geloadeten
	 * BetreuungspensumContainers geladen werden, da danach keine Session mehr zur Verfuegung steht!
	 */
	private void loadRelationsAndDetach(Gesuch gesuch) {
		for (KindContainer kindContainer : gesuch.getKindContainers()) {
			for (Betreuung betreuung : kindContainer.getBetreuungen()) {
				betreuung.getBetreuungspensumContainers().size();
				betreuung.getAbwesenheitContainers().size();
			}
		}
		if (gesuch.getGesuchsteller1() != null) {
			gesuch.getGesuchsteller1().getErwerbspensenContainers().size();
			gesuch.getGesuchsteller1().getAdressen().size();
		}
		if (gesuch.getGesuchsteller2() != null) {
			gesuch.getGesuchsteller2().getErwerbspensenContainers().size();
			gesuch.getGesuchsteller2().getAdressen().size();
		}
		persistence.getEntityManager().detach(gesuch);
	}
}
