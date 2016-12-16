package ch.dvbern.ebegu.entities;

import ch.dvbern.ebegu.types.DateRange;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * Entity fuer Abwesenheit.
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
@Audited
@Entity
public class Abwesenheit extends AbstractDateRangedEntity implements Comparable<Abwesenheit> {

	private static final long serialVersionUID = -6776981643150835840L;

	public Abwesenheit() {
	}


	@Override
	public int compareTo(Abwesenheit o) {
		CompareToBuilder builder = new CompareToBuilder();
		builder.append(this.getGueltigkeit(), o.getGueltigkeit());
		builder.append(this.getId(), o.getId());
		return builder.toComparison();
	}

	public Abwesenheit copyForMutation(Abwesenheit mutation) {
		return (Abwesenheit) super.copyForMutation(mutation);
	}
}