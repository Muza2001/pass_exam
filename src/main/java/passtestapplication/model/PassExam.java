package passtestapplication.model;

import lombok.*;
import org.hibernate.Hibernate;
import passtestapplication.model.temp.AbcEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PassExam extends AbcEntity {

    @ManyToMany
    List<Test> tests = new ArrayList<>();

    private String fullName;

    private Character answer;

    private Instant created_at;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PassExam passExam = (PassExam) o;
        return getId() != null && Objects.equals(getId(), passExam.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
