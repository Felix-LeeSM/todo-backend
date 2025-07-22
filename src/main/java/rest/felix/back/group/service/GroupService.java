package rest.felix.back.group.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.DetailedGroupDTO;
import rest.felix.back.group.dto.FullGroupDetailsDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.GroupInvitationDTO;
import rest.felix.back.group.dto.GroupInvitationInfoDTO;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.group.dto.UserGroupDTO;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.exception.GroupNotFoundException;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.dto.TodoCountDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.exception.UserAccessDeniedException;
import rest.felix.back.user.repository.UserRepository;

@Service
@AllArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final UserGroupRepository userGroupRepository;
  private final TodoRepository todoRepository;
  private final UserRepository userRepository;

  @Transactional
  public GroupDTO createGroup(CreateGroupDTO createGroupDTO) {

    GroupDTO groupDTO = groupRepository.createGroup(createGroupDTO);

    userGroupRepository.registerUserToGroup(
        createGroupDTO.getUserId(), groupDTO.getId(), GroupRole.OWNER);

    return groupDTO;
  }

  @Transactional(readOnly = true)
  public List<GroupDTO> findGroupsByUserId(long userId) {

    return groupRepository.findGroupsByUserId(userId);
  }

  @Transactional(readOnly = true)
  public List<DetailedGroupDTO> findDetailedGroupsByUserId(long userId) {
    List<GroupDTO> myGroups = groupRepository.findGroupsByUserId(userId);
    if (myGroups.isEmpty()) return List.of();

    List<Long> groupIds = myGroups.stream().map(GroupDTO::getId).toList();

    Map<Long, TodoCountDTO> todoCountDTOs = todoRepository.findTodoCountsByGroupIds(groupIds);
    Map<Long, List<MemberDTO>> memberDTOs = userRepository.findMembersByGroupIds(groupIds);
    Map<Long, GroupRole> myRoles = userGroupRepository.findUserRolesByGroupIds(userId, groupIds);

    return myGroups.stream()
        .map(
            group -> {
              long groupId = group.getId();
              TodoCountDTO todoCount =
                  todoCountDTOs.getOrDefault(groupId, new TodoCountDTO(groupId, 0, 0));
              List<MemberDTO> members = memberDTOs.getOrDefault(groupId, List.of());
              GroupRole myRole = myRoles.get(groupId);

              return new DetailedGroupDTO(
                  groupId,
                  group.getName(),
                  group.getDescription(),
                  todoCount.getTodoCount(),
                  todoCount.getCompletedTodoCount(),
                  members,
                  members.size(),
                  myRole);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<GroupDTO> findById(long groupId) {

    return groupRepository.findById(groupId);
  }

  @Transactional(readOnly = true)
  public Optional<GroupRole> findUserRole(long userId, long groupId) {
    return userGroupRepository
        .findByUserIdAndGroupId(userId, groupId)
        .map(UserGroupDTO::getGroupRole);
  }

  @Transactional
  public void deleteGroupById(long groupId) {
    userGroupRepository.deleteByGroupId(groupId);
    todoRepository.deleteByGroupId(groupId);

    groupRepository.deleteGroupById(groupId);
  }

  @Transactional(readOnly = true)
  public FullGroupDetailsDTO findFullDetailedGroupById(long userId, long groupId)
      throws RuntimeException {

    GroupRole myRole =
        userGroupRepository
            .findByUserIdAndGroupId(userId, groupId)
            .map(UserGroupDTO::getGroupRole)
            .orElseThrow(UserAccessDeniedException::new);
    GroupDTO groupDTO =
        groupRepository.findById(groupId).orElseThrow(ResourceNotFoundException::new);

    List<MemberDTO> memberDTOs = userRepository.findMembersByGroupId(groupId);

    List<TodoDTO> todoDTOs = todoRepository.findByGroupId(groupId);

    return new FullGroupDetailsDTO(
        groupId,
        groupDTO.getName(),
        groupDTO.getDescription(),
        memberDTOs,
        memberDTOs.size(),
        myRole,
        todoDTOs);
  }

  @Transactional(readOnly = true)
  public void assertGroupAuthority(long userId, long groupId, GroupRole groupRole) {

    userGroupRepository
        .findByUserIdAndGroupId(userId, groupId)
        .map(dto -> dto.getGroupRole())
        .filter(role -> role.gte(groupRole))
        .orElseThrow(UserAccessDeniedException::new);
  }

  @Transactional
  public void registerUserToGroup(long userId, long groupId, GroupRole groupRole) {
    userGroupRepository.registerUserToGroup(userId, groupId, groupRole);
  }

  @Transactional(readOnly = true)
  public GroupInvitationInfoDTO findGroupInvitationInfo(GroupInvitationDTO groupInvitation) {
    long groupId = groupInvitation.getGroupId();
    long issuerId = groupInvitation.getIssuerId();

    GroupDTO groupDTO = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);

    TodoCountDTO todoCount =
        todoRepository.findTodoCountsByGroupId(groupId).orElseThrow(GroupNotFoundException::new);

    List<MemberDTO> members = userRepository.findMembersByGroupId(groupId);

    MemberDTO issuer =
        members.stream()
            .filter(member -> member.getId() == issuerId)
            .findFirst()
            .orElseThrow(ResourceNotFoundException::new);

    ZonedDateTime activeUntil = groupInvitation.getExpiresAt();

    return new GroupInvitationInfoDTO(
        groupDTO.getName(),
        groupDTO.getDescription(),
        todoCount.getTodoCount(),
        todoCount.getCompletedTodoCount(),
        members.size(),
        issuer,
        members,
        activeUntil);
  }
}
