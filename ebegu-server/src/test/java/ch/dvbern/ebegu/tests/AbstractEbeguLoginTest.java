package ch.dvbern.ebegu.tests;

import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.tets.TestDataUtil;
import ch.dvbern.ebegu.tets.util.JBossLoginContextFactory;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Diese Klasse loggt vor jeder testmethode als superadmin ein und danach wieder aus.
 * Zudem wird der superadmin in der dp erstellt
 */
public abstract class AbstractEbeguLoginTest extends AbstractEbeguTest {


	private static final Logger LOG = LoggerFactory.getLogger(AbstractEbeguLoginTest.class);
	private  LoginContext loginContext;

	@Inject
	private Persistence<Gesuch> persistence;
	private Benutzer dummyAdmin;

	@Before
	public  void performLogin() {
		dummyAdmin = TestDataUtil.createDummySuperAdmin(persistence);
		try {
			loginContext = JBossLoginContextFactory.createLoginContext("superadmin", "superadmin");
			loginContext.login();
		} catch (LoginException ex) {
			LOG.error("Konnte dummy login nicht vornehmen fuer ArquillianTests ", ex);
		}
	}

	@After
	public  void performLogout() {
		try {
			if (loginContext != null) {
				loginContext.logout();
			}
		} catch (LoginException e) {
			LOG.error("Konnte dummy loginnicht ausloggen ", e);
		}
	}

	public Benutzer getDummySuperadmin() {
		return dummyAdmin;
	}
}