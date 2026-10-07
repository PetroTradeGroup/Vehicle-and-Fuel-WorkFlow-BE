package zw.co.petrotrade.workflow.user.dto;

import zw.co.petrotrade.workflow.user.Role;

import java.util.List;

public record CurrentUserResponse(UserResponse user, List<Role> roles) {
}
