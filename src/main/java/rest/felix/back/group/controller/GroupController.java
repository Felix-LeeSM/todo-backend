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
import rest.felix.back.group.dto.FullGroupDetailsDTO;
import rest.felix.back.group.dto.FullGroupDetailsResponseDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.GroupResponseDTO;
import rest.felix.back.group.service.GroupService;
import rest.felix.back.user.dto.AuthUserDTO;
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

    long userId = authUser.getUserId();

    CreateGroupDTO createGroupDTO =
        new CreateGroupDTO(
            userId, createGroupRequestDTO.getName(), createGroupRequestDTO.getDescription());

    GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

    GroupResponseDTO groupResponseDTO =
        new GroupResponseDTO(groupDTO.getId(), groupDTO.getName(), groupDTO.getDescription());

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

  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {
    groupService.assertCanDeleteGroup(groupId, groupId);

    groupService.deleteGroupById(groupId);

    return ResponseEntity.noContent().build();
  }
}
