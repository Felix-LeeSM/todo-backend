package rest.felix.back.group.controller;

import jakarta.validation.Valid;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rest.felix.back.common.config.GroupConfig;
import rest.felix.back.group.dto.*;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.exception.*;
import rest.felix.back.group.service.GroupInvitationService;
import rest.felix.back.group.service.GroupService;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.exception.UserAccessDeniedException;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

  private final GroupService groupService;
  private final GroupInvitationService groupInvitationService;

  private final GroupConfig groupConfig;

  @PostMapping
  public ResponseEntity<GroupResponseDTO> createGroup(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @RequestBody @Valid CreateGroupRequestDTO createGroupRequestDTO) {

    long userId = authUser.getUserId();

    CreateGroupDTO createGroupDTO =
        new CreateGroupDTO(
            userId, createGroupRequestDTO.name(), createGroupRequestDTO.description());

    GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

    GroupResponseDTO groupResponseDTO =
        new GroupResponseDTO(groupDTO.id(), groupDTO.name(), groupDTO.description());

    return ResponseEntity.status(HttpStatus.CREATED).body(groupResponseDTO);
  }

  @GetMapping("/my")
  public ResponseEntity<List<DetailedGroupResponseDTO>> getMyDetailedGroups(
      @AuthenticationPrincipal AuthUserDTO authUser) {

    long userId = authUser.getUserId();

    List<DetailedGroupResponseDTO> detailedGroupResponseDTOs =
        groupService.findDetailedGroupsByUserId(userId).stream()
            .map(DetailedGroupResponseDTO::of)
            .toList();

    return ResponseEntity.status(HttpStatus.OK).body(detailedGroupResponseDTOs);
  }

  @GetMapping("/{groupId}")
  public ResponseEntity<FullGroupDetailsResponseDTO> getUserGroup(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {

    long userId = authUser.getUserId();

    FullGroupDetailsDTO groupDTO = groupService.findFullDetailedGroupById(userId, groupId);

    return ResponseEntity.status(HttpStatus.OK).body(FullGroupDetailsResponseDTO.of(groupDTO));
  }

  @PutMapping("/{groupId}")
  public ResponseEntity<GroupResponseDTO> updateGroup(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @RequestBody @Valid UpdateGroupRequestDTO updateGroupRequestDTO) {

    long userId = authUser.getUserId();

    groupService.assertGroupAuthority(userId, groupId, GroupRole.MANAGER);

    UpdateGroupDTO updateGroupDTO = UpdateGroupDTO.of(groupId, updateGroupRequestDTO);

    GroupDTO groupDTO = groupService.updateGroup(updateGroupDTO);

    return ResponseEntity.status(HttpStatus.OK).body(GroupResponseDTO.of(groupDTO));
  }

  @DeleteMapping("/{groupId}/member/{memberId}")
  public ResponseEntity<Void> deleteUserGroup(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "memberId") long memberId) {
    long userId = authUser.getUserId();
    groupService.assertGroupAuthority(userId, groupId, GroupRole.OWNER);
    groupService.findUserRole(memberId, groupId).orElseThrow(MembershipNotFoundException::new);

    if (memberId == userId) throw new CannotRemoveSelfException();

    groupService.deleteUserGroupById(memberId, groupId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PatchMapping("/{groupId}/member/{memberId}")
  public ResponseEntity<Void> updateUserGroup(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "memberId") long memberId,
      @RequestBody @Valid UpdateMemberRequestDTO updateMemberRequestDTO) {
    long userId = authUser.getUserId();
    groupService.assertGroupAuthority(userId, groupId, GroupRole.MANAGER);

    GroupRole userRole =
        groupService.findUserRole(userId, groupId).orElseThrow(UserAccessDeniedException::new);
    GroupRole memberRole =
        groupService.findUserRole(memberId, groupId).orElseThrow(MembershipNotFoundException::new);

    if (userId == memberId
        || userRole.lte(updateMemberRequestDTO.role())
        || userRole.lte(memberRole)) throw new ForbiddenRoleChangeException();

    UpdateMemberDTO updateMemberDTO = UpdateMemberDTO.of(memberId, groupId, updateMemberRequestDTO);
    groupService.updateUserGroup(updateMemberDTO);

    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {
    long userId = authUser.getUserId();
    groupService.assertGroupAuthority(userId, groupId, GroupRole.OWNER);

    groupService.deleteGroupById(groupId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{groupId}/invitation")
  public ResponseEntity<CreateGroupInvitationResponseDTO> createGroupInvitation(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {
    long userId = authUser.getUserId();
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    groupService.assertGroupAuthority(userId, groupId, GroupRole.MANAGER);
    groupInvitationService.assertInvitationCountLimitation(
        userId, groupId, groupConfig.getLimit(), now);

    // 기본적으로 하루만 지속되는 토큰을 만듦
    String token = groupInvitationService.createInvitationToken();
    ZonedDateTime expiresAt = now.plus(1, groupConfig.getLimitUnit().toChronoUnit());

    CreateGroupInvitationDTO createGroupInvitationDTO =
        new CreateGroupInvitationDTO(userId, groupId, token, expiresAt);
    GroupInvitationDTO groupInvitation =
        groupInvitationService.createGroupInvitation(createGroupInvitationDTO);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateGroupInvitationResponseDTO.of(groupInvitation));
  }

  @GetMapping("/invitation/{token}")
  public ResponseEntity<GroupInvitationInfoDTOResponse> getInvitationInfo(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "token") String token) {

    long userId = authUser.getUserId();
    ZonedDateTime now = ZonedDateTime.now();

    GroupInvitationDTO groupInvitation = groupInvitationService.findValidInvitation(token, now);

    if (groupService.findUserRole(userId, groupInvitation.groupId()).isPresent())
      throw new AlreadyGroupMemberException();

    GroupInvitationInfoDTO groupInvitationInfo =
        groupService.findGroupInvitationInfo(groupInvitation);

    return ResponseEntity.ok().body(GroupInvitationInfoDTOResponse.of(groupInvitationInfo));
  }

  @PostMapping("/invitation/{token}/accept")
  public ResponseEntity<Void> acceptInvitation(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "token") String token) {

    long userId = authUser.getUserId();
    ZonedDateTime now = ZonedDateTime.now();

    GroupInvitationDTO groupInvitation = groupInvitationService.findValidInvitation(token, now);

    if (groupService.findUserRole(userId, groupInvitation.groupId()).isPresent())
      throw new AlreadyGroupMemberException();

    groupService.registerUserToGroup(userId, groupInvitation.groupId(), GroupRole.MEMBER);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
