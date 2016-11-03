package ch.dvbern.ebegu.rules;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Familiensituation;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import ch.dvbern.ebegu.enums.AntragTyp;
import ch.dvbern.ebegu.enums.EnumFamilienstatus;
import ch.dvbern.ebegu.enums.EnumGesuchstellerKardinalitaet;
import ch.dvbern.ebegu.tets.TestDataUtil;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;

/**
 * Tests für ErwerbspensumRule
 */
public class ErwerbspensumRuleTest {

	private final LocalDate START_PERIODE = LocalDate.of(2016, Month.AUGUST, 1);
	private final LocalDate ENDE_PERIODE = LocalDate.of(2017, Month.JULY, 31);


	@Test
	public void testKeinErwerbspensum() {
		Betreuung betreuung = createGesuch(true);

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(0, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertFalse(result.get(0).getBemerkungen().isEmpty());
		Assert.assertTrue(result.get(0).getBemerkungen().contains(RuleKey.ERWERBSPENSUM.name()));
	}

	@Test
	public void testNormalfallZweiGesuchsteller() {
		Betreuung betreuung = createGesuch(true);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 100, 0));
		gesuch.getGesuchsteller2().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 40, 0));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(40, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertTrue(result.get(0).getBemerkungen().isEmpty());
	}

	@Test
	public void testNormalfallEinGesuchsteller() {
		Betreuung betreuung = createGesuch(false);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 60, 0));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(60, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertTrue(result.get(0).getBemerkungen().isEmpty());
	}

	@Test
	public void testNurEinErwerbspensumBeiZweiGesuchstellern() throws Exception {
		Betreuung betreuung = createGesuch(true);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 80, 0));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(0, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertFalse(result.get(0).getBemerkungen().isEmpty());
	}

	@Test
	public void testMehrAls100ProzentBeiEinemGesuchsteller() {
		Betreuung betreuung = createGesuch(false);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 100, 10));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(100, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertFalse(result.get(0).getBemerkungen().isEmpty());
	}

	@Test
	public void testMehrAls100ProzentBeiBeidenGesuchstellern() {
		Betreuung betreuung = createGesuch(true);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 100, 10));
		gesuch.getGesuchsteller2().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 100, 10));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(100, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertTrue(result.get(0).getBemerkungen().contains("Erwerbspensum GS 1"));
		Assert.assertTrue(result.get(0).getBemerkungen().contains("Erwerbspensum GS 2"));
	}

	@Test
	public void testFrom1GSTo2GSRechtzeitigEingereicht() {
		Betreuung betreuung = createGesuch(true);

		Gesuch gesuch = betreuung.extractGesuch();
		gesuch.setTyp(AntragTyp.MUTATION);
		gesuch.setEingangsdatum(LocalDate.of(2016, Month.DECEMBER, 26));

		from1GSTo2GS(betreuung, gesuch);

	}

	@Test
	public void testFrom1GSTo2GSSpaetEingereichtAberNiedrigerWert() {
		Betreuung betreuung = createGesuch(true);

		Gesuch gesuch = betreuung.extractGesuch();
		gesuch.setTyp(AntragTyp.MUTATION);
		gesuch.setEingangsdatum(LocalDate.of(2016, Month.MAY, 26));

		from1GSTo2GS(betreuung, gesuch);
	}

	private void from1GSTo2GS(Betreuung betreuung, Gesuch gesuch) {
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 90, 0));
		gesuch.getGesuchsteller2().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 80, 0));

		gesuch.setFamiliensituationErstgesuch(new Familiensituation());
		gesuch.getFamiliensituationErstgesuch().setFamilienstatus(EnumFamilienstatus.ALLEINERZIEHEND);
		gesuch.getFamiliensituationErstgesuch().setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ALLEINE);
		gesuch.getFamiliensituation().setFamilienstatus(EnumFamilienstatus.VERHEIRATET);
		gesuch.getFamiliensituation().setAenderungPer(LocalDate.of(2017, Month.MARCH, 26));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(90, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertEquals(START_PERIODE, result.get(0).getGueltigkeit().getGueltigAb());
		Assert.assertEquals(LocalDate.of(2017, Month.MARCH, 25), result.get(0).getGueltigkeit().getGueltigBis());

		Assert.assertEquals(70, result.get(1).getAnspruchberechtigtesPensum());
		Assert.assertEquals(LocalDate.of(2017, Month.MARCH, 26), result.get(1).getGueltigkeit().getGueltigAb());
		Assert.assertEquals(ENDE_PERIODE, result.get(1).getGueltigkeit().getGueltigBis());
	}

	@Test
	public void testFrom2GSTo1GSRechtzeitigEingereicht() {
		Betreuung betreuung = createGesuch(true);

		Gesuch gesuch = betreuung.extractGesuch();
		gesuch.setTyp(AntragTyp.MUTATION);
		gesuch.setEingangsdatum(LocalDate.of(2016, Month.DECEMBER, 26));

		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 90, 0));
		gesuch.getGesuchsteller2().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 70, 0));

		gesuch.setFamiliensituationErstgesuch(new Familiensituation());
		gesuch.getFamiliensituationErstgesuch().setFamilienstatus(EnumFamilienstatus.VERHEIRATET);
		gesuch.getFamiliensituation().setFamilienstatus(EnumFamilienstatus.ALLEINERZIEHEND);
		gesuch.getFamiliensituation().setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ALLEINE);
		gesuch.getFamiliensituation().setAenderungPer(LocalDate.of(2017, Month.MARCH, 26));

		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(60, result.get(0).getAnspruchberechtigtesPensum());
		Assert.assertEquals(START_PERIODE, result.get(0).getGueltigkeit().getGueltigAb());
		Assert.assertEquals(LocalDate.of(2017, Month.MARCH, 25), result.get(0).getGueltigkeit().getGueltigBis());

		Assert.assertEquals(90, result.get(1).getAnspruchberechtigtesPensum());
		Assert.assertEquals(LocalDate.of(2017, Month.MARCH, 26), result.get(1).getGueltigkeit().getGueltigAb());
		Assert.assertEquals(ENDE_PERIODE, result.get(1).getGueltigkeit().getGueltigBis());
	}


	/**
	 * das Pensum muss wie folgt abgerundet werden:
	 * X0 - X4 = X0
	 * X5 - X9 = Y0, wo Y=X+1
	 * @throws Exception
     */
	@Test
	public void testRoundToTens() throws Exception {
		Betreuung betreuung = createGesuch(false);
		Gesuch gesuch = betreuung.extractGesuch();

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, -1, 0));
		List<VerfuegungZeitabschnitt> result = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(0, result.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 0, 0));
		List<VerfuegungZeitabschnitt> result2 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(0, result2.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 1, 0));
		List<VerfuegungZeitabschnitt> result3 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(0, result3.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 50, 0));
		List<VerfuegungZeitabschnitt> result4 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(50, result4.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 51, 0));
		List<VerfuegungZeitabschnitt> result5 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(50, result5.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 54, 0));
		List<VerfuegungZeitabschnitt> result6 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(50, result6.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 55, 0));
		List<VerfuegungZeitabschnitt> result7 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(60, result7.get(0).getAnspruchberechtigtesPensum());

		//mit zuschlag
		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 50, 5));
		List<VerfuegungZeitabschnitt> result8 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(60, result8.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 59, 0));
		List<VerfuegungZeitabschnitt> result9 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(60, result9.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 99, 0));
		List<VerfuegungZeitabschnitt> result10 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(100, result10.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 100, 0));
		List<VerfuegungZeitabschnitt> result11 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(100, result11.get(0).getAnspruchberechtigtesPensum());

		gesuch.getGesuchsteller1().setErwerbspensenContainers(new HashSet<>());
		gesuch.getGesuchsteller1().addErwerbspensumContainer(TestDataUtil.createErwerbspensum(START_PERIODE, ENDE_PERIODE, 101, 0));
		List<VerfuegungZeitabschnitt> result12 = EbeguRuleTestsHelper.calculate(betreuung);
		Assert.assertEquals(100, result12.get(0).getAnspruchberechtigtesPensum());

	}



	private Betreuung createGesuch(final boolean gs2) {
		final Betreuung betreuung = TestDataUtil.createGesuchWithBetreuungspensum(gs2);
		final Gesuch gesuch = betreuung.extractGesuch();

		TestDataUtil.createDefaultAdressenForGS(gesuch, gs2);

		return betreuung;
	}

}
