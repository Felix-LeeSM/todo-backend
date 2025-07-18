package rest.felix.back.group.entity.enumerated;

public enum GroupRole {
  OWNER(400),
  MANAGER(300),
  MEMBER(200),
  VIEWER(100);

  private final int authorityLevel;

  GroupRole(int level) {
    this.authorityLevel = level;
  }

  public boolean gt(GroupRole other) {
    return this.authorityLevel > other.authorityLevel;
  }

  public boolean gte(GroupRole other) {
    return this.authorityLevel >= other.authorityLevel;
  }

  public boolean lt(GroupRole other) {
    return this.authorityLevel < other.authorityLevel;
  }

  public boolean lte(GroupRole other) {
    return this.authorityLevel <= other.authorityLevel;
  }

  public boolean eq(GroupRole other) {
    return this.authorityLevel == other.authorityLevel;
  }
}
