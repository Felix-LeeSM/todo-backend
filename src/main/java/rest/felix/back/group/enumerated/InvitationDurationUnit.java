package rest.felix.back.group.enumerated;

import java.time.temporal.ChronoUnit;

public enum InvitationDurationUnit {
  DAYS(ChronoUnit.DAYS),
  HOURS(ChronoUnit.HOURS);

  private final ChronoUnit chronoUnit;

  InvitationDurationUnit(ChronoUnit chronoUnit) {
    this.chronoUnit = chronoUnit;
  }

  public ChronoUnit toChronoUnit() {
    return chronoUnit;
  }
}
