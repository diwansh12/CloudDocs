package com.clouddocs.backend.entity;

import jakarta.persistence.*;

@Embeddable
public class WorkflowCondition {
    @Column(name = "condition_field")
    private String field; // e.g., "fileSize", "category", "uploadedBy.role"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_operator")
    private ConditionOperator operator; // EQUALS, GREATER_THAN, LESS_THAN, CONTAINS, etc.
    
    @Column(name = "condition_value")
    private String value; // The value to compare against
    
    // Constructors
    public WorkflowCondition() {}
    
    public WorkflowCondition(String field, ConditionOperator operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
    
    // Getters and Setters
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    
    public ConditionOperator getOperator() { return operator; }
    public void setOperator(ConditionOperator operator) { this.operator = operator; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

