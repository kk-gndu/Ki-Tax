/*
 * Copyright © 2016 DV Bern AG, Switzerland
 *
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschützt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulässig. Dies gilt
 * insbesondere für Vervielfältigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht übergeben, ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 */

package ch.dvbern.ebegu.reporting.lib;
import javax.annotation.Nonnull;

public class ExcelTemplateParseException extends Exception {
	private static final long serialVersionUID = 8625035601352241549L;

	public ExcelTemplateParseException(@Nonnull String message, @Nonnull Throwable cause) {
		super(message, cause);
	}

	public ExcelTemplateParseException(@Nonnull String message) {
		super(message);
	}
}