package passtestapplication.model;


import lombok.*;
import org.hibernate.Hibernate;
import passtestapplication.model.temp.AbcEntity;

import javax.persistence.Entity;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Test extends AbcEntity {


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Test test = (Test) o;
        return getId() != null && Objects.equals(getId(), test.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
