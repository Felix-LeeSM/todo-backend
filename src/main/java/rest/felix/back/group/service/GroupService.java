package rest.felix.back.group.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.UserGroupDTO;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.exception.UserAccessDeniedException;

@Service
@Transactional
@AllArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final UserGroupRepository userGroupRepository;
  private final TodoRepository todoRepository;

  public GroupDTO createGroup(CreateGroupDTO createGroupDTO) {

    GroupDTO groupDTO = groupRepository.createGroup(createGroupDTO);

    userGroupRepository.registerUserToGroup(
        createGroupDTO.getUserId(), groupDTO.getId(), GroupRole.OWNER);

    return groupDTO;
  }

  public List<GroupDTO> getGroupsByUserId(long userId) {

    return groupRepository.getGroupsByUserId(userId);
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
