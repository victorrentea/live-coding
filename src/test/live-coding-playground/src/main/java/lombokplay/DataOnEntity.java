package lombokplay;

import lombok.Data;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

@Data
@Entity
public class DataOnEntity {

    @Id
    private Long id;

    private String name;

    @ElementCollection
    private List<String> children;
}
