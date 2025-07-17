package rest.felix.back.group.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.dto.DetailedGroupResponseDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.GroupResponseDTO;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.service.GroupService;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UserAccessDeniedException;
import rest.felix.back.user.service.UserService;

@RestController
@RequestMapping("/api/v1/group")
@AllArgsConstructor
public class GroupController {

  private final UserService userService;
  private final GroupService groupService;

  @PostMapping
  public ResponseEntity<GroupResponseDTO> createGroup(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @RequestBody @Valid CreateGroupRequestDTO createGroupRequestDTO) {

    UserDTO userDTO =
        userService.getById(authUser.getUserId()).orElseThrow(NoMatchingUserException::new);

    CreateGroupDTO createGroupDTO =
        new CreateGroupDTO(
            userDTO.getId(),
            createGroupRequestDTO.getName(),
            createGroupRequestDTO.getDescription());

    GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

    GroupResponseDTO groupResponseDTO =
        new GroupResponseDTO(groupDTO.getId(), groupDTO.getName(), groupDTO.getDescription());

    return ResponseEntity.status(HttpStatus.CREATED).body(groupResponseDTO);
  }

  @GetMapping("/my")
  public ResponseEntity<List<DetailedGroupResponseDTO>> getMyDetailedGroups(
      @AuthenticationPrincipal AuthUserDTO authUser) {

    UserDTO userDTO =
        userService.getById(authUser.getUserId()).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    List<DetailedGroupResponseDTO> detailedGroupResponseDTOs =
        groupService.findDetailedGroupsByUserId(userId).stream()
            .map(
                dto ->
                    new DetailedGroupResponseDTO(
                        dto.getId(),
                        dto.getName(),
                        dto.getDescription(),
                        dto.getTodoCount(),
                        dto.getCompletedTodoCount(),
                        dto.getMembers(),
                        dto.getMemberCount(),
                        dto.getMyRole()))
            .toList();

    return ResponseEntity.status(HttpStatus.OK).body(detailedGroupResponseDTOs);
  }

  @GetMapping("/{groupId}")
  public ResponseEntity<GroupResponseDTO> getUserGroup(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {

    long userId = authUser.getUserId();
    userService.getById(userId).orElseThrow(NoMatchingUserException::new);

    groupService.getUserRoleInGroup(userId, groupId);

    GroupDTO groupDTO = groupService.getGroupById(groupId);
    GroupResponseDTO groupResponseDTO =
        new GroupResponseDTO(groupDTO.getId(), groupDTO.getName(), groupDTO.getDescription());

    return ResponseEntity.status(HttpStatus.OK).body(groupResponseDTO);
  }

  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {

    UserDTO userDTO =
        userService.getById(authUser.getUserId()).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    GroupRole groupRole = groupService.getUserRoleInGroup(userId, groupId);

    if (groupRole != GroupRole.OWNER) {
      throw new UserAccessDeniedException();
    }

    groupService.deleteGroupById(groupId);

    return ResponseEntity.noContent().build();
  }
}
