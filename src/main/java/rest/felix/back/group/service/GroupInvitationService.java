package rest.felix.back.group.service;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.group.dto.CreateGroupInvitationDTO;
import rest.felix.back.group.dto.GroupInvitationDTO;
import rest.felix.back.group.entity.GroupInvitation;
import rest.felix.back.group.exception.ExpiredInvitationException;
import rest.felix.back.group.exception.NoInvitationException;
import rest.felix.back.group.exception.TooManyInvitationsException;
import rest.felix.back.group.repository.GroupInvitationRepository;

@Service
@RequiredArgsConstructor
public class GroupInvitationService {

  private final GroupInvitationRepository groupInvitationRepository;

  public String createInvitationToken() {
    return UUID.randomUUID().toString();
  }

  @Transactional(readOnly = true)
  public void assertInvitationCountLimitation(
      long issuerId, long groupId, Long limit, ZonedDateTime now) {

    if (limit <= groupInvitationRepository.countActiveUntil(issuerId, groupId, now))
      throw new TooManyInvitationsException();
  }

  @Transactional
  public GroupInvitationDTO createGroupInvitation(
      CreateGroupInvitationDTO createGroupInvitationDTO) {
    return groupInvitationRepository.createGroupInvitation(createGroupInvitationDTO);
  }

  @Transactional
  public void deleteInvitation(GroupInvitation groupInvitation) {
    groupInvitationRepository.delete(groupInvitation);
  }

  @Transactional(readOnly = true)
  public GroupInvitationDTO findValidInvitation(String token, ZonedDateTime now) {
    GroupInvitationDTO groupInvitation =
        groupInvitationRepository.findByToken(token).orElseThrow(NoInvitationException::new);
    if (groupInvitation.expiresAt().isBefore(ZonedDateTime.now()))
      throw new ExpiredInvitationException();

    return groupInvitation;
  }

  @Transactional(readOnly = true)
  public GroupInvitationDTO findInvitation(String token) {
    return groupInvitationRepository.findByToken(token).orElseThrow(NoInvitationException::new);
  }
}
