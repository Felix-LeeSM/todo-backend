package rest.felix.back.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import rest.felix.back.group.enumerated.InvitationDurationUnit;

@Getter
@ConfigurationProperties(prefix = "group.invitation")
public class GroupConfig {

  private final Long limit;
  private final InvitationDurationUnit limitUnit;

  public GroupConfig(
      @DefaultValue("10") Long limit, @DefaultValue("DAYS") InvitationDurationUnit limitUnit) {
    this.limit = limit;
    this.limitUnit = limitUnit;
  }
}
