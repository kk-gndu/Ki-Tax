package ch.dvbern.ebegu.rules;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.KindContainer;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import ch.dvbern.ebegu.enums.Kinderabzug;
import ch.dvbern.ebegu.types.DateRange;
import ch.dvbern.ebegu.util.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Umsetzung der ASIV Revision
 * <p>
 * 2. Immer aktuelle Familiengrösse
 * <p>
 * Gem. neuer ASIV Verordnung müssen die Kinder für die Berechnung der Familiengrösse ab dem Beginn den Monats NACH dem
 * Ereigniseintritt (e.g. Geburt) berücksichtigt werden. Dasselbe gilt bei der Aenderung des Zivilstands. Bei einer Mutation
 * der Familiensituation ist das Datum "Aendern per" relevant.
 */
public class FamilienabzugAbschnittRule extends AbstractAbschnittRule {

	private final Logger LOG = LoggerFactory.getLogger(FamilienabzugAbschnittRule.class.getSimpleName());

	private final BigDecimal pauschalabzugProPersonFamiliengroesse3;
	private final BigDecimal pauschalabzugProPersonFamiliengroesse4;
	private final BigDecimal pauschalabzugProPersonFamiliengroesse5;
	private final BigDecimal pauschalabzugProPersonFamiliengroesse6;

	public FamilienabzugAbschnittRule(DateRange validityPeriod,
									  BigDecimal pauschalabzugProPersonFamiliengroesse3,
									  BigDecimal pauschalabzugProPersonFamiliengroesse4,
									  BigDecimal pauschalabzugProPersonFamiliengroesse5,
									  BigDecimal pauschalabzugProPersonFamiliengroesse6) {
		super(RuleKey.FAMILIENSITUATION, RuleType.GRUNDREGEL_DATA, validityPeriod);
		this.pauschalabzugProPersonFamiliengroesse3 = pauschalabzugProPersonFamiliengroesse3;
		this.pauschalabzugProPersonFamiliengroesse4 = pauschalabzugProPersonFamiliengroesse4;
		this.pauschalabzugProPersonFamiliengroesse5 = pauschalabzugProPersonFamiliengroesse5;
		this.pauschalabzugProPersonFamiliengroesse6 = pauschalabzugProPersonFamiliengroesse6;
	}

	//TODO (gapa) Achtung, bei einer Aenderung der Familiensituation durch eine Mutation muss noch das Datum der Mutation berücksichtigt werden! Mutationen werden spaeter behandelt

	@Override
	@Nonnull
	protected List<VerfuegungZeitabschnitt> createVerfuegungsZeitabschnitte(@Nonnull Betreuung betreuung, @Nonnull List<VerfuegungZeitabschnitt> zeitabschnitte) {

		Gesuch gesuch = betreuung.extractGesuch();
		final List<VerfuegungZeitabschnitt> familienAbzugZeitabschnitt = createInitialenFamilienAbzug(gesuch);

		Map<LocalDate, Double> famGrMap = new TreeMap<>();

		//Suchen aller Geburtstage innerhalb der Gesuchsperiode und speichern in der Liste mit Familiengrösse
		for (KindContainer kindContainer : gesuch.getKindContainers()) {
			final LocalDate geburtsdatum = kindContainer.getKindJA().getGeburtsdatum();
			if (gesuch.getGesuchsperiode().getGueltigkeit().contains(geburtsdatum)) {
				final LocalDate beginMonatNachGeb = geburtsdatum.plusMonths(1).withDayOfMonth(1);
				famGrMap.put(beginMonatNachGeb, calculateFamiliengroesse(gesuch, beginMonatNachGeb));
			}
		}

		if (gesuch.extractFamiliensituation() != null && gesuch.extractFamiliensituation().getAenderungPer() != null) {
			// die familiensituation aendert sich jetzt erst ab dem naechsten Monat, deswegen .plusMonths(1).withDayOfMonth(1)
			final LocalDate aenderungPerBeginningNextMonth = gesuch.extractFamiliensituation().getAenderungPer().plusMonths(1).withDayOfMonth(1);
			famGrMap.put(aenderungPerBeginningNextMonth, calculateFamiliengroesse(gesuch, aenderungPerBeginningNextMonth));
		}

		// aufsteigend durch die Geburtstage gehen und immer den letzen Abschnitt  unterteilen in zwei Abschnitte
		for (Map.Entry<LocalDate, Double> entry : famGrMap.entrySet()) {
			final VerfuegungZeitabschnitt lastVerfuegungZeitabschnitt = familienAbzugZeitabschnitt.get(familienAbzugZeitabschnitt.size() - 1);
			lastVerfuegungZeitabschnitt.getGueltigkeit().setGueltigBis(entry.getKey().minusDays(1));

			final VerfuegungZeitabschnitt verfuegungZeitabschnitt = new VerfuegungZeitabschnitt();
			verfuegungZeitabschnitt.getGueltigkeit().setGueltigAb(entry.getKey());
			verfuegungZeitabschnitt.getGueltigkeit().setGueltigBis(gesuch.getGesuchsperiode().getGueltigkeit().getGueltigBis());
			verfuegungZeitabschnitt.setAbzugFamGroesse(calculateAbzugAufgrundFamiliengroesse(entry.getValue()));
			verfuegungZeitabschnitt.setFamGroesse(new BigDecimal(String.valueOf(entry.getValue())));

			familienAbzugZeitabschnitt.add(verfuegungZeitabschnitt);
		}

		return familienAbzugZeitabschnitt;
	}

	public List<VerfuegungZeitabschnitt> createInitialenFamilienAbzug(Gesuch gesuch) {
		List<VerfuegungZeitabschnitt> initialFamAbzugList = new ArrayList<>();
		VerfuegungZeitabschnitt initialFamAbzug = new VerfuegungZeitabschnitt(gesuch.getGesuchsperiode().getGueltigkeit());
		//initial gilt die Familiengroesse die am letzten Tag vor dem Start der neuen Gesuchsperiode vorhanden war
		double familiengroesse = gesuch.getGesuchsperiode() == null ? 0 : calculateFamiliengroesse(gesuch, gesuch.getGesuchsperiode().getGueltigkeit().getGueltigAb());
		BigDecimal abzugAufgrundFamiliengroesse = getAbzugFamGroesse(gesuch, familiengroesse);
		initialFamAbzug.setAbzugFamGroesse(abzugAufgrundFamiliengroesse);
		initialFamAbzug.setFamGroesse(new BigDecimal(String.valueOf(familiengroesse)));

		initialFamAbzugList.add(initialFamAbzug);
		return initialFamAbzugList;
	}

	@Override
	public boolean isRelevantForFamiliensituation() {
		return true;
	}

	private BigDecimal getAbzugFamGroesse(Gesuch gesuch, double familiengroesse) {
		return gesuch.getGesuchsperiode() == null ? BigDecimal.ZERO :
			calculateAbzugAufgrundFamiliengroesse(familiengroesse);
	}


	/**
	 * Die Familiengroesse wird folgendermassen kalkuliert:
	 * Familiengrösse = Gesuchsteller1 + Gesuchsteller2 (falls vorhanden) + Faktor Steuerabzug pro Kind (0, 0.5, oder 1)
	 * <p>
	 * Der Faktor wird gemaess Wert des Felds kinderabzug von Kind berechnet:
	 * KEIN_ABZUG = 0
	 * HALBER_ABZUG = 0.5
	 * GANZER_ABZUG = 1
	 * KEINE_STEUERERKLAERUNG = 1
	 * <p>
	 * Nur die Kinder die vor dem uebergebenen Datum geboren sind werden mitberechnet
	 * <p>
	 *
	 * @param gesuch das Gesuch aus welchem die Daten geholt werden
	 * @param date   das Datum fuer das die familiengroesse kalkuliert werden muss
	 * @return die familiengroesse als double
	 */
	double calculateFamiliengroesse(Gesuch gesuch, @Nullable LocalDate date) {
		double familiengroesse = 0;
		if (gesuch != null) {

			if (gesuch.extractFamiliensituation() != null) { // wenn die Familiensituation nicht vorhanden ist, kann man nichts machen (die Daten wurden falsch eingegeben)
				if (gesuch.extractFamiliensituationErstgesuch() != null && date != null && (
					gesuch.extractFamiliensituation().getAenderungPer() == null //wenn aenderung per nicht gesetzt ist nehmen wir wert aus erstgesuch
					|| date.isBefore(gesuch.extractFamiliensituation().getAenderungPer().plusMonths(1).withDayOfMonth(1)))) {

					familiengroesse = familiengroesse + (gesuch.extractFamiliensituationErstgesuch().hasSecondGesuchsteller() ? 2 : 1);
				} else {
					familiengroesse = familiengroesse + (gesuch.extractFamiliensituation().hasSecondGesuchsteller() ? 2 : 1);
				}
			} else{
				LOG.warn("Die Familiengroesse kann noch nicht richtig berechnet werden weil die Familiensituation nicht richtig ausgefuellt ist. Antragnummer: {}" , gesuch.getAntragNummer() );
			}

			for (KindContainer kindContainer : gesuch.getKindContainers()) {
				if (kindContainer.getKindJA() != null && (date == null || kindContainer.getKindJA().getGeburtsdatum().isBefore(date))) {
					if (kindContainer.getKindJA().getKinderabzug() == Kinderabzug.HALBER_ABZUG) {
						familiengroesse += 0.5;
					}
					if (kindContainer.getKindJA().getKinderabzug() == Kinderabzug.GANZER_ABZUG || kindContainer.getKindJA().getKinderabzug() == Kinderabzug.KEINE_STEUERERKLAERUNG) {
						familiengroesse++;
					}
				}
			}
		}
		return familiengroesse;
	}

	BigDecimal calculateAbzugAufgrundFamiliengroesse(double familiengroesse) {

		BigDecimal abzugFromServer = BigDecimal.ZERO;
		if (familiengroesse < 3) {
			// Unter 3 Personen gibt es keinen Abzug!
			abzugFromServer = BigDecimal.ZERO;
		} else if (familiengroesse < 4) {
			abzugFromServer = pauschalabzugProPersonFamiliengroesse3;
		} else if (familiengroesse < 5) {
			abzugFromServer = pauschalabzugProPersonFamiliengroesse4;
		} else if (familiengroesse < 6) {
			abzugFromServer = pauschalabzugProPersonFamiliengroesse5;
		} else if (familiengroesse >= 6) {
			abzugFromServer = pauschalabzugProPersonFamiliengroesse6;
		}

		// Ein Bigdecimal darf nicht aus einem double erzeugt werden, da das Ergebnis nicht genau die gegebene Nummer waere
		// deswegen muss man hier familiengroesse als String uebergeben. Sonst bekommen wir PMD rule AvoidDecimalLiteralsInBigDecimalConstructor
		// Wir runden die Zahl ausserdem zu einer Ganzzahl weil wir fuer das Massgebende einkommen mit Ganzzahlen rechnen
		return MathUtil.GANZZAHL.from(new BigDecimal(String.valueOf(familiengroesse)).multiply(abzugFromServer));
	}
}