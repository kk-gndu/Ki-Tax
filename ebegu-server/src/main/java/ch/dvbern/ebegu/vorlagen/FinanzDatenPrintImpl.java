package ch.dvbern.ebegu.vorlagen;
/*
* Copyright (c) 2016 DV Bern AG, Switzerland
*
* Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
* geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
* insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
* elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
* Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
*
* Ersteller: zeab am: 23.08.2016
*/

import java.math.BigDecimal;

import ch.dvbern.ebegu.entities.AbstractFinanzielleSituation;

/**
 * Implementiert den FinanzielleSituationPrint
 */
public class FinanzDatenPrintImpl implements FinanzDatenPrint {

	private FinanzSituationGesuchsteller fsGesuchsteller1;
	private FinanzSituationGesuchsteller fsGesuchsteller2;

	/**
	 * Konstruktor
	 *
	 * @param finanzielleSituationG1 {@link AbstractFinanzielleSituation}
	 * @param finanzielleSituationG2 {@link AbstractFinanzielleSituation}
	 */
	public FinanzDatenPrintImpl(FinanzSituationGesuchsteller fsGesuchsteller1, FinanzSituationGesuchsteller fsGesuchsteller2) {

		this.fsGesuchsteller1 = fsGesuchsteller1;
		this.fsGesuchsteller2 = fsGesuchsteller2;

	}

	@Override
	public BigDecimal getNettolohnG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getNettolohn();
	}

	@Override
	public BigDecimal getNettolohnG2() {

		return fsGesuchsteller2.getFinanzielleSituation().getNettolohn();
	}

	@Override
	public BigDecimal getFamilienzulagenG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getFamilienzulage();
	}

	@Override
	public BigDecimal getFamilienzulagenG2() {

		return fsGesuchsteller2 != null ? fsGesuchsteller2.getFinanzielleSituation().getFamilienzulage() : null;
	}

	@Override
	public BigDecimal getErsatzeinkommenG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getErsatzeinkommen();
	}

	@Override
	public BigDecimal getErsatzeinkommenG2() {

		return fsGesuchsteller2 != null ? fsGesuchsteller2.getFinanzielleSituation().getErsatzeinkommen() : null;
	}

	@Override
	public BigDecimal getUnterhaltsbeitraegeG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getErhalteneAlimente();
	}

	@Override
	public BigDecimal getUnterhaltsbeitraegeG2() {

		return fsGesuchsteller2 != null ? fsGesuchsteller2.getFinanzielleSituation().getErhalteneAlimente() : null;
	}

	@Override
	public BigDecimal getGeschaeftsgewinnG1() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getGeschaeftsgewinnG2() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZwischentotalEinkuenfteG1() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZwischentotalEinkuenfteG2() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getTotalEinkuenfte() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getBruttovermoegenG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getBruttovermoegen();
	}

	@Override
	public BigDecimal getBruttovermoegenG2() {

		return fsGesuchsteller2.getFinanzielleSituation() != null ? fsGesuchsteller2.getFinanzielleSituation().getBruttovermoegen() : null;
	}

	@Override
	public BigDecimal getSchuldenG1() {

		return fsGesuchsteller1.getFinanzielleSituation().getSchulden();
	}

	@Override
	public BigDecimal getSchuldenG2() {

		return null;
	}

	@Override
	public BigDecimal getZwischentotalNettovermoegenBeiderGesuchsteller1() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZwischentotalNettovermoegenBeiderGesuchsteller2() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZwischentotalNettovermoegenInsgesamt() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getNettovermoegen() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getAbzuegeBeiEinerFamiliengroesseVon5Personen() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public int getAnzahlPersonen() {

		// TODO Implementieren
		return 9;
	}

	@Override
	public BigDecimal getTotalAbzuege() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZusammenzugTotaleinkuenfte() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZusammenzugNettovermoegen() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getZusammenzugTotalAbzuege() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}

	@Override
	public BigDecimal getMassgebendesEinkommen() {

		// TODO Implementieren
		return new BigDecimal(11111);
	}
}
