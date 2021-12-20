package playground;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;


@Entity
public class DataOnEntity {

    @Id
    private Long id;

    private String name;

    @ToString.Exclude
    @ElementCollection
    private List<String> children;
}
