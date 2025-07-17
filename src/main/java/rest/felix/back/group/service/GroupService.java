package rest.felix.back.group.service;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.DetailedGroupDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.group.dto.UserGroupDTO;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.dto.TodoCountDTO;
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

  public GroupDTO createGroup(CreateGroupDTO createGroupDTO) {

    GroupDTO groupDTO = groupRepository.createGroup(createGroupDTO);

    userGroupRepository.registerUserToGroup(
        createGroupDTO.getUserId(), groupDTO.getId(), GroupRole.OWNER);

    return groupDTO;
  }

  @Transactional(readOnly = true)
  public List<GroupDTO> getGroupsByUserId(long userId) {

    return groupRepository.getGroupsByUserId(userId);
  }

  @Transactional(readOnly = true)
  public List<DetailedGroupDTO> findDetailedGroupsByUserId(long userId) {
    List<GroupDTO> myGroups = groupRepository.getGroupsByUserId(userId);
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

  public GroupDTO getGroupById(long groupId) {

    return groupRepository.getById(groupId).orElseThrow(ResourceNotFoundException::new);
  }

  public GroupRole getUserRoleInGroup(long userId, long groupId) {
    return userGroupRepository
        .getByUserIdAndGroupId(userId, groupId)
        .map(UserGroupDTO::getGroupRole)
        .orElseThrow(UserAccessDeniedException::new);
  }

  public void deleteGroupById(long groupId) {
    userGroupRepository.deleteByGroupId(groupId);
    todoRepository.deleteByGroupId(groupId);

    groupRepository.deleteGroupById(groupId);
  }
}
