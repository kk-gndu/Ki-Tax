package ch.dvbern.ebegu.dto;

import ch.dvbern.ebegu.entities.AbstractFinanzielleSituation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO für die Resultate der Berechnungen der Finanziellen Situation
 */
public class AbstractFinanzielleSituationResultateDTO {

	private BigDecimal geschaeftsgewinnDurchschnittGesuchsteller1 = BigDecimal.ZERO;
	private BigDecimal geschaeftsgewinnDurchschnittGesuchsteller2 = BigDecimal.ZERO;
	private BigDecimal einkommenBeiderGesuchsteller = BigDecimal.ZERO;
	private BigDecimal nettovermoegenFuenfProzent = BigDecimal.ZERO;
	private BigDecimal anrechenbaresEinkommen = BigDecimal.ZERO;
	private BigDecimal abzuegeBeiderGesuchsteller = BigDecimal.ZERO;
	private BigDecimal abzugAufgrundFamiliengroesse = BigDecimal.ZERO;
	private BigDecimal totalAbzuege = BigDecimal.ZERO;
	private BigDecimal massgebendesEinkommen = BigDecimal.ZERO;
	private Double familiengroesse;


	AbstractFinanzielleSituationResultateDTO(double familiengroesse, BigDecimal famGroesseAbz) {
		this.familiengroesse = familiengroesse;
		this.abzugAufgrundFamiliengroesse = famGroesseAbz;
	}

	protected BigDecimal calcGeschaeftsgewinnDurchschnitt(AbstractFinanzielleSituation abstractFinanzielleSituation) {
		if (abstractFinanzielleSituation != null) {
			BigDecimal total = BigDecimal.ZERO;
			BigDecimal anzahlJahre = BigDecimal.ZERO;
			if (abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus2() != null) {
				total = total.add(abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus2());
				anzahlJahre = anzahlJahre.add(BigDecimal.ONE);
			}
			if (abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus1() != null) {
				total = total.add(abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus1());
				anzahlJahre = anzahlJahre.add(BigDecimal.ONE);
			}
			if (abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahr() != null) {
				total = total.add(abstractFinanzielleSituation.getGeschaeftsgewinnBasisjahr());
				anzahlJahre = anzahlJahre.add(BigDecimal.ONE);
			}
			if (anzahlJahre.intValue() > 0) {
				return total.divide(anzahlJahre, RoundingMode.HALF_UP);
			}
		}
		return null;
	}


	private BigDecimal calcVermoegen5Prozent(AbstractFinanzielleSituation abstractFinanzielleSituation) {
		BigDecimal total = subtract(abstractFinanzielleSituation.getBruttovermoegen(), abstractFinanzielleSituation.getSchulden());
		if (total.compareTo(BigDecimal.ZERO) < 0) {
			total = BigDecimal.ZERO;
		}
		total = percent(total, 5);
		return total;
	}

	protected BigDecimal add(BigDecimal value1, BigDecimal value2) {
		value1 = value1 != null ? value1 : BigDecimal.ZERO;
		value2 = value2 != null ? value2 : BigDecimal.ZERO;
		return value1.add(value2);
	}

	private BigDecimal subtract(BigDecimal value1, BigDecimal value2) {
		value1 = value1 != null ? value1 : BigDecimal.ZERO;
		value2 = value2 != null ? value2 : BigDecimal.ZERO;
		return value1.subtract(value2);
	}

	private BigDecimal percent(BigDecimal value, int percent) {
		BigDecimal total = value != null ? value : BigDecimal.ZERO;
		total = total.multiply(new BigDecimal("" + percent));
		total = total.divide(new BigDecimal("100"), RoundingMode.HALF_UP);
		return total;
	}

	/**
	 * rundet auf die naechste Ganzzahl groesser gleich 0
	 */
	private BigDecimal positiveNonNullAndRound(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		// Returns the maximum of this BigDecimal and val.
		value = value.setScale(0, RoundingMode.HALF_UP);
		return value.max(BigDecimal.ZERO);
	}

	private BigDecimal calcEinkommen(AbstractFinanzielleSituation abstractFinanzielleSituation, BigDecimal nettoJahresLohn) {
		BigDecimal total = BigDecimal.ZERO;
		total = add(total, nettoJahresLohn);
		total = add(total, abstractFinanzielleSituation.getFamilienzulage());
		total = add(total, abstractFinanzielleSituation.getErsatzeinkommen());
		total = add(total, abstractFinanzielleSituation.getErhalteneAlimente());
		total = add(total, calcGeschaeftsgewinnDurchschnitt(abstractFinanzielleSituation));
		return total;
	}

	void calculateZusammen() {
		this.anrechenbaresEinkommen = add(einkommenBeiderGesuchsteller, nettovermoegenFuenfProzent);
		this.totalAbzuege = add(abzuegeBeiderGesuchsteller, abzugAufgrundFamiliengroesse);
		this.massgebendesEinkommen = subtract(anrechenbaresEinkommen, totalAbzuege);
	}

	void initToZero() {
		// Alle Werte auf 0 initialisieren, falls Null
		// Wenn negativ -> 0
		geschaeftsgewinnDurchschnittGesuchsteller1 = positiveNonNullAndRound(geschaeftsgewinnDurchschnittGesuchsteller1);
		geschaeftsgewinnDurchschnittGesuchsteller2 = positiveNonNullAndRound(geschaeftsgewinnDurchschnittGesuchsteller2);
		einkommenBeiderGesuchsteller = positiveNonNullAndRound(einkommenBeiderGesuchsteller);
		nettovermoegenFuenfProzent = positiveNonNullAndRound(nettovermoegenFuenfProzent);
		anrechenbaresEinkommen = positiveNonNullAndRound(anrechenbaresEinkommen);
		abzuegeBeiderGesuchsteller = positiveNonNullAndRound(abzuegeBeiderGesuchsteller);
		abzugAufgrundFamiliengroesse = positiveNonNullAndRound(abzugAufgrundFamiliengroesse);
		totalAbzuege = positiveNonNullAndRound(totalAbzuege);
		massgebendesEinkommen = positiveNonNullAndRound(massgebendesEinkommen);
	}


	void calculateProGesuchsteller(AbstractFinanzielleSituation finanzielleSituationGS, BigDecimal nettoJahresLohn) {
		if (finanzielleSituationGS != null) {
			this.einkommenBeiderGesuchsteller = add(einkommenBeiderGesuchsteller, calcEinkommen(finanzielleSituationGS, nettoJahresLohn));
			this.nettovermoegenFuenfProzent = add(nettovermoegenFuenfProzent, calcVermoegen5Prozent(finanzielleSituationGS));
			this.abzuegeBeiderGesuchsteller = add(abzuegeBeiderGesuchsteller, finanzielleSituationGS.getGeleisteteAlimente());
		}
	}


	public BigDecimal getGeschaeftsgewinnDurchschnittGesuchsteller1() {
		return geschaeftsgewinnDurchschnittGesuchsteller1;
	}

	public void setGeschaeftsgewinnDurchschnittGesuchsteller1(BigDecimal geschaeftsgewinnDurchschnittGesuchsteller1) {
		this.geschaeftsgewinnDurchschnittGesuchsteller1 = geschaeftsgewinnDurchschnittGesuchsteller1;
	}

	public BigDecimal getGeschaeftsgewinnDurchschnittGesuchsteller2() {
		return geschaeftsgewinnDurchschnittGesuchsteller2;
	}

	public void setGeschaeftsgewinnDurchschnittGesuchsteller2(BigDecimal geschaeftsgewinnDurchschnittGesuchsteller2) {
		this.geschaeftsgewinnDurchschnittGesuchsteller2 = geschaeftsgewinnDurchschnittGesuchsteller2;
	}

	public BigDecimal getEinkommenBeiderGesuchsteller() {
		return einkommenBeiderGesuchsteller;
	}

	public void setEinkommenBeiderGesuchsteller(BigDecimal einkommenBeiderGesuchsteller) {
		this.einkommenBeiderGesuchsteller = einkommenBeiderGesuchsteller;
	}

	public BigDecimal getNettovermoegenFuenfProzent() {
		return nettovermoegenFuenfProzent;
	}

	public void setNettovermoegenFuenfProzent(BigDecimal nettovermoegenFuenfProzent) {
		this.nettovermoegenFuenfProzent = nettovermoegenFuenfProzent;
	}

	public BigDecimal getAnrechenbaresEinkommen() {
		return anrechenbaresEinkommen;
	}

	public void setAnrechenbaresEinkommen(BigDecimal anrechenbaresEinkommen) {
		this.anrechenbaresEinkommen = anrechenbaresEinkommen;
	}

	public BigDecimal getAbzuegeBeiderGesuchsteller() {
		return abzuegeBeiderGesuchsteller;
	}

	public void setAbzuegeBeiderGesuchsteller(BigDecimal abzuegeBeiderGesuchsteller) {
		this.abzuegeBeiderGesuchsteller = abzuegeBeiderGesuchsteller;
	}

	public BigDecimal getAbzugAufgrundFamiliengroesse() {
		return abzugAufgrundFamiliengroesse;
	}

	public void setAbzugAufgrundFamiliengroesse(BigDecimal abzugAufgrundFamiliengroesse) {
		this.abzugAufgrundFamiliengroesse = abzugAufgrundFamiliengroesse;
	}

	public BigDecimal getTotalAbzuege() {
		return totalAbzuege;
	}

	public void setTotalAbzuege(BigDecimal totalAbzuege) {
		this.totalAbzuege = totalAbzuege;
	}

	public BigDecimal getMassgebendesEinkommen() {
		return massgebendesEinkommen;
	}

	public void setMassgebendesEinkommen(BigDecimal massgebendesEinkommen) {
		this.massgebendesEinkommen = massgebendesEinkommen;
	}

	public Double getFamiliengroesse() {
		return familiengroesse;
	}

	public void setFamiliengroesse(Double familiengroesse) {
		this.familiengroesse = familiengroesse;
	}
}