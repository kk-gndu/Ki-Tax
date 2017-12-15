/*
 * Ki-Tax: System for the management of external childcare subsidies
 * Copyright (C) 2017 City of Bern Switzerland
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.dvbern.ebegu.enums;

/**
 * Enum fuer moegliche Errors
 */
public enum ErrorCodeEnum {

	ERROR_ENTITY_NOT_FOUND,
	ERROR_PARAMETER_NOT_FOUND,
	ERROR_TOO_MANY_RESULTS,
	ERROR_PRINT_PDF,
	ERROR_MAIL,
	ERROR_EXISTING_ONLINE_MUTATION,
	ERROR_EXISTING_ERNEUERUNGSGESUCH,
	ERROR_ZAHLUNG_ERSTELLEN,
	ERROR_ONLY_VERFUEGT_ALLOWED,
	ERROR_ONLY_IN_BEARBEITUNG_STV_ALLOWED,
	ERROR_ONLY_IN_GEPRUEFT_STV_ALLOWED,
	ERROR_ONLY_IN_GEPRUEFT_ALLOWED,
	ERROR_ONLY_IF_NO_BETERUUNG,
	ERROR_PERSONENSUCHE_BUSINESS,
	ERROR_PERSONENSUCHE_TECHNICAL,
	ERROR_NOT_FROM_STATUS_BESCHWERDE,
	ERROR_GESUCHSPERIODE_INVALID_STATUSUEBERGANG,
	ERROR_GESUCHSPERIODE_CANNOT_BE_REMOVED,
	ERROR_GESUCHSPERIODE_CANNOT_BE_CLOSED,
	ERROR_MUTATIONSMELDUNG_FALL_GESPERRT,
	ERROR_MUTATIONSMELDUNG_GESUCH_NICHT_FREIGEGEBEN,
	ERROR_MUTATIONSMELDUNG_STATUS_VERFUEGEN,
	ERROR_VORGAENGER_MISSING,
	ERROR_ANTRAG_NOT_COMPLETE,
	ERROR_EXISTING_MAHNUNG,
	ERROR_INVALID_EBEGUSTATE,
	ERROR_FREIGABEQUITTUNG_PAPIER,
	ERROR_DELETION_NOT_ALLOWED_FOR_JA,
	ERROR_DELETION_NOT_ALLOWED_FOR_GS,
	ERROR_DELETION_ANTRAG_NOT_ALLOWED,
	ERROR_UPLOAD_INVALID_FILETYPE,
	ERROR_FIN_SIT_IS_NOT_REQUIRED
}
