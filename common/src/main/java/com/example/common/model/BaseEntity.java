package com.example.common.model;

import com.example.common.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseEntity {

   // @CreatedDate
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdDateTime;

   // @LastModifiedDate
    @Column()
    protected LocalDateTime updatedAt;

   // @CreatedBy
    @Column(updatable = false)
    protected String createdBy;

  //  @LastModifiedBy
    protected String updatedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    protected RecordStatus recordStatus = RecordStatus.ACTIVE;

    // বিজনেস লজিক অনুযায়ী এক্সট্রা ফিল্ড
    protected String actedUserName;
}
