package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.entities.Zahlungsauftrag;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

/**
 * Service fuer die Verwaltung von Benutzern
 */
public interface Pain001Service {

	String getPainFileContent(Zahlungsauftrag zahlungsauftrag);

}
