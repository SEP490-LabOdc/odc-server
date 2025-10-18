package com.odc.userservice.entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Yêu cầu nhập role name")
    @Size(max = 50, message = "Role name không được vượt quá 50 ký tự")
    private String name;

    @Column(length = 255)
    @Size(max = 255, message = "Description không được vượt quá 255 ký tự")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Set<String>> permissions;

    @OneToMany(mappedBy = "role")
    private Set<User> users;
}
