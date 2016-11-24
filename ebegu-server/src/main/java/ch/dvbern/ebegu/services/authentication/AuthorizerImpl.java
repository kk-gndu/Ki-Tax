package ch.dvbern.ebegu.services.authentication;

import ch.dvbern.ebegu.authentication.PrincipalBean;
import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.UserRole;
import ch.dvbern.ebegu.services.Authorizer;
import ch.dvbern.ebegu.services.FallService;
import ch.dvbern.ebegu.services.InstitutionService;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.EJBAccessException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;

import static ch.dvbern.ebegu.enums.UserRole.*;

/**
 * Authorizer Implementation
 */
@RequestScoped
public class AuthorizerImpl implements Authorizer {

	private static final Logger LOG = LoggerFactory.getLogger(AuthorizerImpl.class);


	private static final UserRole[] JA_OR_ADM = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA};
	private static final UserRole[] ALL_EXCEPT_INST_TRAEG = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, REVISOR, JURIST, SCHULAMT, STEUERAMT};

	@Inject
	private PrincipalBean principalBean;

	@Inject
	private Persistence<Gesuch> persistence;

	@Inject
	private FallService fallService;

	@Inject
	private InstitutionService institutionService;

	@Override
	public void checkReadAuthorizationGesuchId(@Nullable String gesuchId) {
		if (gesuchId != null) {
			LOG.warn("homa: Ineffiziente Authorisierungspruefung. Gesuchid sollte moeglichst nicht verwendet werden");
			checkReadAuthorization(getGesuchById(gesuchId));
		}
	}

	@Override
	public void checkReadAuthorization(@Nullable Gesuch gesuch) {
		if (gesuch != null) {
			boolean allowed = isReadAuthorized(gesuch);
			if (!allowed) {
				throwViolation(gesuch);
			}
		}
	}

	@Override
	public void checkReadAuthorizationGesuche(@Nullable Collection<Gesuch> gesuche) {
		if (gesuche != null) {
			gesuche.forEach(this::checkReadAuthorization);
		}
	}


	@Override
	public void checkCreateAuthorizationGesuch() {
		if (principalBean.isCallerInAnyOfRole(GESUCHSTELLER, SACHBEARBEITER_JA, ADMIN, SUPER_ADMIN)) {
			return;
		}
		throwCreateViolation();
	}

	@Override
	public void checkCreateAuthorizationFinSit(@Nonnull FinanzielleSituationContainer finanzielleSituation) {
		if (principalBean.isCallerInAnyOfRole(ADMIN, SUPER_ADMIN)) {
			return;
		}
		if (principalBean.isCallerInRole(GESUCHSTELLER)) {
			//gesuchsteller darf nur welche machen wenn nicht mutation, ausserdem muss ihm das zugehoerige geusch gehoeren
			boolean isMutation = finanzielleSituation.getVorgaengerId() != null;
			String parentOwner = finanzielleSituation.getGesuchsteller().getUserErstellt() != null ? finanzielleSituation.getGesuchsteller().getUserErstellt() : "";
			if (isMutation || !parentOwner.equals(principalBean.getPrincipal().getName())) {
				throwCreateViolation();
			}
		}
	}

	@Override
	public void checkReadAuthorizationFall(String fallId) {
		Optional<Fall> fallOptional = fallService.findFall(fallId);
		if (fallOptional.isPresent()) {
			Fall fall = fallOptional.get();
			checkReadAuthorizationFall(fall);
		}
	}

	@Override
	public void checkReadAuthorizationFall(@Nullable Fall fall) {
		boolean allowed = isReadAuthorizedFall(fall);
		if (!allowed) {
			throwViolation(fall);
		}
	}

	@Override
	public void checkReadAuthorizationFaelle(Collection<Fall> faelle) {
		if (faelle != null) {
			faelle.forEach(this::checkReadAuthorizationFall);
		}
	}

	private boolean isReadAuthorizedFall(@Nullable Fall fall) {
		if (fall == null) {
			return true;
		}

		validateMandantMatches(fall);
		if (principalBean.isCallerInAnyOfRole(SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, SACHBEARBEITER_TRAEGERSCHAFT, SACHBEARBEITER_INSTITUTION)) {
			return true;
		}

		//noinspection RedundantIfStatement
		if (principalBean.isCallerInRole(GESUCHSTELLER.name())
			&& (fall.getUserErstellt() != null && fall.getUserErstellt().equals(principalBean.getPrincipal().getName()))) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("PMD.CollapsibleIfStatements")
	private void validateMandantMatches(@Nullable HasMandant mandantEntity) {
		//noinspection ConstantConditions
		if (mandantEntity == null || mandantEntity.getMandant() == null) {
			return;
		}
		Mandant mandant = mandantEntity.getMandant();
		if (!mandant.equals(principalBean.getMandant())) {
			if (!principalBean.isCallerInRole(SUPER_ADMIN)) {
				throwMandantViolation(mandantEntity); // super admin darf auch wenn er keinen mandant hat
			}
		}
	}

	@Override
	public void checkWriteAuthorization(@Nullable Fall fall) {
		if (fall != null) {
			boolean allowed = isWriteAuthorized(fall, principalBean.getPrincipal().getName());
			if (!allowed) {
				throwViolation(fall);
			}
		}
	}

	@Override
	@SuppressWarnings("PMD.CollapsibleIfStatements")
	public void checkWriteAuthorizationFall(@Nonnull String fallId) {
		Optional<Fall> fallOptional = fallService.findFall(fallId);
		if (!fallOptional.isPresent()) {
			return;
		}
		Fall fall = fallOptional.get();
		checkWriteAuthorization(fall);
	}

	@Override
	public void checkWriteAuthorization(Gesuch gesuch) throws EJBAccessException {
		boolean allowed = isWriteAuthorized(gesuch, principalBean.getPrincipal().getName());
		if (!allowed) {
			throwViolation(gesuch);
		}
	}

	@Override
	public void checkWriteAuthorization(Verfuegung verfuegung) {
		//nur sachbearbeiter ja und admins duefen verfuegen
		if (!principalBean.isCallerInAnyOfRole(SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA)) {
			throwViolation(verfuegung);
		}
	}

	@Override
	public void checkWriteAuthorization(@Nullable FinanzielleSituationContainer finanzielleSituation) {
		if (finanzielleSituation == null) {
			return;
		}
		String name = principalBean.getPrincipal().getName();
		boolean writeAllowed = isWriteAuthorized(finanzielleSituation, name);
		boolean isMutation = finanzielleSituation.getVorgaengerId() != null;

		if (!writeAllowed || (isMutation && principalBean.isCallerInRole(GESUCHSTELLER))) {
			throwViolation(finanzielleSituation);
		}
	}

	@Override
	public void checkReadAuthorization(Betreuung betr) {
		boolean allowed = isReadAuthorized(betr);
		if (!allowed) {
			throwViolation(betr);
		}
	}

	@Override
	public void checkReadAuthorizationForAllBetreuungen(@Nullable Collection<Betreuung> betreuungen) {
		if (betreuungen != null) {
			betreuungen.stream()
				.filter(betreuung -> !isReadAuthorized(betreuung))
				.findAny()
				.ifPresent(this::throwViolation);
		}
	}

	@Override
	public void checkReadAuthorizationForAnyBetreuungen(@Nullable Collection<Betreuung> betreuungen) {
		if (betreuungen != null && betreuungen.stream().noneMatch(this::isReadAuthorized)) {
			throw new EJBAccessException(
				"Access Violation"
					+ " user is not allowed for any of these betreuungen");
		}
	}

	@Override
	public void checkReadAuthorization(Verfuegung verfuegung) {
		if (verfuegung != null) {
			//an betreuung delegieren
			checkReadAuthorization(verfuegung.getBetreuung());
		}
	}

	@Override
	public void checkReadAuthorization(@Nullable WizardStep step) {
		if (step != null) {
			checkReadAuthorization(step.getGesuch());
		}
	}

	@Override
	public void checkReadAuthorizationVerfuegungen(Collection<Verfuegung> verfuegungen) {
		if (verfuegungen != null) {
			verfuegungen.forEach(this::checkReadAuthorization);
		}
	}

	@Override
	public void checkWriteAuthorization(Betreuung betreuungToRemove) {
		boolean allowed = isWriteAuthorized(betreuungToRemove, principalBean.getPrincipal().getName());
		if (!allowed) {
			throwViolation(betreuungToRemove);
		}
	}

	@Override
	public void checkReadAuthorization(@Nullable ErwerbspensumContainer ewpCnt) {
		if (ewpCnt != null) {
			UserRole[] allowedRoles = {SACHBEARBEITER_JA, SUPER_ADMIN, ADMIN, REVISOR, JURIST};
			boolean allowed = isInRoleOrGSOwner(allowedRoles, ewpCnt, principalBean.getPrincipal().getName());
			if (!allowed) {
				throwViolation(ewpCnt);
			}
		}
	}

	@Override
	public void checkReadAuthorization(@Nullable FinanzielleSituationContainer finanzielleSituation) {
		if (finanzielleSituation != null) {
			// hier fuer alle lesbar ausser fuer institution/traegerschaft
			String name = principalBean.getPrincipal().getName();
			boolean allowed = isInRoleOrGSOwner(ALL_EXCEPT_INST_TRAEG, finanzielleSituation, name);
			if (!allowed) {
				throwViolation(finanzielleSituation);
			}
		}
	}

	@Override
	public void checkReadAuthorization(@Nonnull Collection<FinanzielleSituationContainer> finanzielleSituationen) {
		finanzielleSituationen.forEach(this::checkReadAuthorization);
	}

	private boolean isInRoleOrGSOwner(UserRole[] allowedRoles, AbstractEntity entity, String principalName) {

		if (principalBean.isCallerInAnyOfRole(allowedRoles)) {
			return true;
		}

		if (principalBean.isCallerInRole(GESUCHSTELLER.name())
			&& (entity.getUserErstellt() == null || entity.getUserErstellt().equals(principalName))) {
			return true;
		}
		return false;
	}

	private boolean isReadAuthorized(Betreuung betreuung) {
		boolean isOwnerOrAdmin = isInRoleOrGSOwner(JA_OR_ADM, betreuung, principalBean.getPrincipal().getName());
		if (isOwnerOrAdmin) {
			return true;
		}

		if (principalBean.isCallerInRole(SACHBEARBEITER_INSTITUTION)) {
			Institution institution = principalBean.getBenutzer().getInstitution();
			Validate.notNull(institution, "Institution des Sachbearbeiters muss gesetzt sein " + principalBean.getBenutzer());
			return betreuung.getInstitutionStammdaten().getInstitution().equals(institution);
		}
		if (principalBean.isCallerInRole(SACHBEARBEITER_TRAEGERSCHAFT)) {
			Traegerschaft traegerschaft = principalBean.getBenutzer().getTraegerschaft();
			Validate.notNull(traegerschaft, "Traegerschaft des des Sachbearbeiters muss gesetzt sein " + principalBean.getBenutzer());
			Collection<Institution> institutions = institutionService.getAllInstitutionenFromTraegerschaft(traegerschaft.getId());
			Institution instToMatch = betreuung.getInstitutionStammdaten().getInstitution();
			return institutions.stream().anyMatch(instToMatch::equals);
		}
		return false;

	}

	@Override
	public void checkReadAuthorizationFinSit(@Nullable Gesuch gesuch) {
		if (gesuch != null) {
			FinanzielleSituationContainer finSitGs1 = gesuch.getGesuchsteller1() != null ? gesuch.getGesuchsteller1().getFinanzielleSituationContainer() : null;
			FinanzielleSituationContainer finSitGs2 = gesuch.getGesuchsteller2() != null ? gesuch.getGesuchsteller2().getFinanzielleSituationContainer() : null;
			checkReadAuthorization(finSitGs1);
			checkReadAuthorization(finSitGs2);
		}
	}

	private boolean isReadAuthorized(Gesuch entity) {
		boolean isOwnerOrAdmin = isInRoleOrGSOwner(JA_OR_ADM, entity, principalBean.getPrincipal().getName());
		if (isOwnerOrAdmin) {
			return true;
		}
		if (principalBean.isCallerInRole(SACHBEARBEITER_INSTITUTION)) {
			Institution institution = principalBean.getBenutzer().getInstitution();
			Validate.notNull(institution, "Institution des Sachbearbeiters muss gesetzt sein " + principalBean.getBenutzer());
			return entity.hasBetreuungOfInstitution(institution); //@reviewer: oder besser ueber service ?
		}
		if (principalBean.isCallerInRole(SACHBEARBEITER_TRAEGERSCHAFT)) {
			Traegerschaft traegerschaft = principalBean.getBenutzer().getTraegerschaft();
			Validate.notNull(traegerschaft, "Traegerschaft des des Sachbearbeiters muss gesetzt sein " + principalBean.getBenutzer());
			Collection<Institution> institutions = institutionService.getAllInstitutionenFromTraegerschaft(traegerschaft.getId());
			return institutions.stream().anyMatch(entity::hasBetreuungOfInstitution);  // irgend eine der betreuungen des gesuchs matched
		}
		if (principalBean.isCallerInRole(SCHULAMT)) {
			return entity.hasBetreuungOfSchulamt();
		}
		return false;
	}


	private boolean isWriteAuthorized(AbstractEntity entity, String principalName) {
		return isInRoleOrGSOwner(JA_OR_ADM, entity, principalName);
	}


	private void throwCreateViolation() {
		throw new EJBAccessException(
			"Access Violation"
				+ " user is not allowed to create entity:"
				+ " for current user: " + principalBean.getPrincipal()
		);
	}

	private void throwViolation(AbstractEntity abstractEntity) {
		throw new EJBAccessException(
			"Access Violation"
				+ " for Entity: " + abstractEntity.getClass().getSimpleName() + "(id=" + abstractEntity.getId() + "):"
				+ " for current user: " + principalBean.getPrincipal()
				+ " in role(s): " + principalBean.discoverRoles()
		);
	}

	private void throwMandantViolation(HasMandant mandantEntity) {
		throw new EJBAccessException(
			"Mandant Access Violation"
				+ " for Entity: " + mandantEntity.getClass().getSimpleName() + "(id=" + mandantEntity.getId() + "):"
				+ " for current user: " + principalBean.getPrincipal()
		);
	}

	public Gesuch getGesuchById(String gesuchID) {
		return persistence.find(Gesuch.class, gesuchID);
	}
}
