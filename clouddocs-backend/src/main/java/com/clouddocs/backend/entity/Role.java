package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.clouddocs.backend.converter.ERoleConverter;

@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Convert(converter = ERoleConverter.class)
@Enumerated(EnumType.STRING)
@Column(length = 20, unique = true)
private ERole name;

    
    @Column(length = 100)
    private String description;
    
    // Bidirectional mapping (optional)
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
    
    // Constructors
    public Role() {}
    
    public Role(ERole name) {
        this.name = name;
    }
    
    public Role(ERole name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ERole getName() { return name; }
    public void setName(ERole name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name == role.name;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return name != null ? name.name() : "UNKNOWN";
    }

    
}
