package com.clouddocs.backend.entity;

public enum ApprovalPolicy {
    UNANIMOUS,    // All approvers must approve
    MAJORITY,     // >50% must approve
    ANY_ONE,
    ALL,      // Any single approval progresses
    QUORUM        // Uses requiredApprovals count
}

