package zw.co.petrotrade.workflow.user.dto;

import zw.co.petrotrade.workflow.user.Role;

// What non-admins may know about a colleague: enough to pick or show them, no contact or HR details
public record UserDirectoryEntry(Long id, String fullName, Role role) {
}
